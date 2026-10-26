package com.giit.campusbite;

import com.giit.campusbite.CatalogDtos.AddonRequest;
import com.giit.campusbite.CatalogDtos.AddonResponse;
import com.giit.campusbite.CatalogDtos.CategoryRequest;
import com.giit.campusbite.CatalogDtos.CategoryResponse;
import com.giit.campusbite.CatalogDtos.ProductRequest;
import com.giit.campusbite.CatalogDtos.ProductResponse;
import com.giit.campusbite.CatalogDtos.VariantRequest;
import com.giit.campusbite.CatalogDtos.VariantResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CatalogService {

    private static final int MAX_OPTIONS_PER_PRODUCT = 30;

    private final FoodCategoryRepository categories;
    private final FoodItemRepository foods;
    private final FoodVariantRepository variants;
    private final FoodAddonRepository addons;

    public CatalogService(FoodCategoryRepository categories,
                          FoodItemRepository foods,
                          FoodVariantRepository variants,
                          FoodAddonRepository addons) {
        this.categories = categories;
        this.foods = foods;
        this.variants = variants;
        this.addons = addons;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories(boolean includeInactive) {
        List<FoodCategory> result = includeInactive
                ? categories.findAllByOrderByDisplayOrderAscNameAsc()
                : categories.findAllByActiveTrueOrderByDisplayOrderAscNameAsc();
        return result.stream().map(this::toCategoryResponse).toList();
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        String name = normalizeName(request.name());
        ensureCategoryNameAvailable(name, null);
        FoodCategory category = new FoodCategory(
                name, clean(request.description()), order(request.displayOrder()));
        if (request.active() != null) {
            category.setActive(request.active());
        }
        return toCategoryResponse(categories.save(category));
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        FoodCategory category = findCategory(id);
        String name = normalizeName(request.name());
        ensureCategoryNameAvailable(name, id);
        category.setName(name);
        category.setDescription(clean(request.description()));
        category.setDisplayOrder(order(request.displayOrder()));
        if (request.active() != null) {
            category.setActive(request.active());
        }
        List<FoodItem> products = foods.findAllByFoodCategory_Id(id);
        products.forEach(food -> food.setCategory(name));
        foods.saveAll(products);
        return toCategoryResponse(categories.save(category));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getProducts(Long categoryId, String search,
                                             boolean includeUnavailable) {
        String query = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        List<FoodItem> products = foods.findAll().stream()
                .filter(food -> includeUnavailable || isPubliclyAvailable(food))
                .filter(food -> categoryId == null
                        || (food.getFoodCategory() != null
                        && categoryId.equals(food.getFoodCategory().getId())))
                .filter(food -> query.isEmpty()
                        || searchableText(food).contains(query))
                .toList();
        if (products.isEmpty()) {
            return List.of();
        }

        List<Long> productIds = products.stream().map(FoodItem::getId).toList();
        Map<Long, List<FoodVariant>> variantsByProduct = variants
                .findAllByFoodItem_IdInOrderByFoodItem_IdAscDisplayOrderAscNameAsc(productIds)
                .stream()
                .collect(Collectors.groupingBy(variant -> variant.getFoodItem().getId()));
        Map<Long, List<FoodAddon>> addonsByProduct = addons
                .findAllByFoodItem_IdInOrderByFoodItem_IdAscDisplayOrderAscNameAsc(productIds)
                .stream()
                .collect(Collectors.groupingBy(addon -> addon.getFoodItem().getId()));
        return products.stream()
                .map(food -> toProductResponse(
                        food, includeUnavailable,
                        variantsByProduct.getOrDefault(food.getId(), List.of()),
                        addonsByProduct.getOrDefault(food.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        FoodItem food = findFood(id);
        if (!isPubliclyAvailable(food)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Menu item not found");
        }
        return toProductResponse(food, false);
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        FoodCategory category = findActiveCategory(request.categoryId());
        FoodItem food = new FoodItem();
        applyProductRequest(food, category, request);
        food = foods.save(food);
        updateVariants(food, request.variants());
        updateAddons(food, request.addons());
        return toProductResponse(food, true);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        FoodItem food = findFood(id);
        FoodCategory category = findActiveCategory(request.categoryId());
        applyProductRequest(food, category, request);
        food = foods.save(food);
        updateVariants(food, request.variants());
        updateAddons(food, request.addons());
        return toProductResponse(food, true);
    }

    private void applyProductRequest(FoodItem food, FoodCategory category,
                                    ProductRequest request) {
        food.setName(request.name().trim());
        food.setFoodCategory(category);
        food.setDescription(clean(request.description()));
        food.setPriceAmount(request.basePrice());
        food.setImageUrl(clean(request.imageUrl()));
        food.setVegetarian(request.vegetarian());
        food.setAvailable(request.available());
    }

    private void updateVariants(FoodItem food, List<VariantRequest> requests) {
        List<VariantRequest> values = requests == null ? List.of() : requests;
        ensureOptionCount(values.size());
        Map<Long, FoodVariant> remaining = new HashMap<>();
        variants.findAllByFoodItem_IdOrderByDisplayOrderAscNameAsc(food.getId())
                .forEach(variant -> remaining.put(variant.getId(), variant));
        Set<String> names = new HashSet<>();
        List<FoodVariant> updated = new ArrayList<>();

        for (VariantRequest request : values) {
            ensureUniqueOptionName(names, request.name());
            FoodVariant variant = request.id() == null
                    ? new FoodVariant(food, request.name().trim(),
                    request.price(), order(request.displayOrder()))
                    : remaining.remove(request.id());
            if (variant == null) {
                throw badRequest("Variant does not belong to this product");
            }
            variant.setFoodItem(food);
            variant.setName(request.name().trim());
            variant.setPrice(request.price());
            variant.setDisplayOrder(order(request.displayOrder()));
            variant.setAvailable(request.available() == null || request.available());
            updated.add(variant);
        }
        remaining.values().forEach(variant -> variant.setAvailable(false));
        variants.saveAll(updated);
        variants.saveAll(remaining.values());
    }

    private void updateAddons(FoodItem food, List<AddonRequest> requests) {
        List<AddonRequest> values = requests == null ? List.of() : requests;
        ensureOptionCount(values.size());
        Map<Long, FoodAddon> remaining = new HashMap<>();
        addons.findAllByFoodItem_IdOrderByDisplayOrderAscNameAsc(food.getId())
                .forEach(addon -> remaining.put(addon.getId(), addon));
        Set<String> names = new HashSet<>();
        List<FoodAddon> updated = new ArrayList<>();

        for (AddonRequest request : values) {
            ensureUniqueOptionName(names, request.name());
            FoodAddon addon = request.id() == null
                    ? new FoodAddon(food, request.name().trim(),
                    request.price(), clean(request.selectionGroup()),
                    order(request.displayOrder()))
                    : remaining.remove(request.id());
            if (addon == null) {
                throw badRequest("Add-on does not belong to this product");
            }
            addon.setFoodItem(food);
            addon.setName(request.name().trim());
            addon.setPrice(request.price());
            addon.setSelectionGroup(clean(request.selectionGroup()));
            addon.setDisplayOrder(order(request.displayOrder()));
            addon.setAvailable(request.available() == null || request.available());
            updated.add(addon);
        }
        remaining.values().forEach(addon -> addon.setAvailable(false));
        addons.saveAll(updated);
        addons.saveAll(remaining.values());
    }

    private CategoryResponse toCategoryResponse(FoodCategory category) {
        return new CategoryResponse(category.getId(), category.getName(),
                category.getDescription(), category.getDisplayOrder(),
                category.isActive(),
                foods.countByFoodCategory_IdAndAvailableTrue(category.getId()));
    }

    private ProductResponse toProductResponse(FoodItem food,
                                               boolean includeUnavailable) {
        return toProductResponse(food, includeUnavailable,
                variants.findAllByFoodItem_IdOrderByDisplayOrderAscNameAsc(food.getId()),
                addons.findAllByFoodItem_IdOrderByDisplayOrderAscNameAsc(food.getId()));
    }

    private ProductResponse toProductResponse(FoodItem food,
                                               boolean includeUnavailable,
                                               List<FoodVariant> foodVariants,
                                               List<FoodAddon> foodAddons) {
        List<VariantResponse> variantResponses =
                foodVariants.stream()
                        .filter(variant -> includeUnavailable || variant.isAvailable())
                        .map(variant -> new VariantResponse(
                                variant.getId(), variant.getName(),
                                variant.getPrice(), variant.getDisplayOrder(),
                                variant.isAvailable()))
                        .toList();
        List<AddonResponse> addonResponses =
                foodAddons.stream()
                        .filter(addon -> includeUnavailable || addon.isAvailable())
                        .map(addon -> new AddonResponse(
                                addon.getId(), addon.getName(), addon.getPrice(),
                                addon.getSelectionGroup(), addon.getDisplayOrder(),
                                addon.isAvailable()))
                        .toList();
        FoodCategory category = food.getFoodCategory();
        return new ProductResponse(
                food.getId(), food.getName(),
                category == null ? null : category.getId(),
                category == null ? food.getCategory() : category.getName(),
                food.getDescription(), food.getPriceAmount(), food.getImageUrl(),
                food.getVegetarian(), food.isAvailable(),
                variantResponses, addonResponses);
    }

    private boolean isPubliclyAvailable(FoodItem food) {
        return food.isAvailable() && food.getFoodCategory() != null
                && food.getFoodCategory().isActive();
    }

    private String searchableText(FoodItem food) {
        return (food.getName() + " " + food.getCategory() + " "
                + (food.getDescription() == null ? "" : food.getDescription()))
                .toLowerCase(Locale.ROOT);
    }

    private FoodCategory findActiveCategory(Long id) {
        FoodCategory category = findCategory(id);
        if (!category.isActive()) {
            throw badRequest("Cannot assign a product to an inactive category");
        }
        return category;
    }

    private FoodCategory findCategory(Long id) {
        return categories.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Category not found"));
    }

    private FoodItem findFood(Long id) {
        return foods.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Menu item not found"));
    }

    private void ensureCategoryNameAvailable(String name, Long currentId) {
        categories.findByNameIgnoreCase(name).ifPresent(existing -> {
            if (!existing.getId().equals(currentId)) {
                throw badRequest("A category with that name already exists");
            }
        });
    }

    private void ensureOptionCount(int count) {
        if (count > MAX_OPTIONS_PER_PRODUCT) {
            throw badRequest("A product can have at most "
                    + MAX_OPTIONS_PER_PRODUCT + " options of each type");
        }
    }

    private void ensureUniqueOptionName(Set<String> names, String name) {
        if (!names.add(normalizeName(name).toLowerCase(Locale.ROOT))) {
            throw badRequest("Option names must be unique within a product");
        }
    }

    private String normalizeName(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private int order(Integer value) {
        return value == null ? 0 : value;
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
