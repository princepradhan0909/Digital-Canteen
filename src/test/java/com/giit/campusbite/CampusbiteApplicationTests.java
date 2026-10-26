package com.giit.campusbite;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:campusbite;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"campusbite.jwt-secret=unit-test-secret-that-is-at-least-thirty-two-characters"
})
@AutoConfigureMockMvc
class CampusbiteApplicationTests {

	@Autowired
	private FoodItemRepository foodItemRepository;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private StudentUserRepository users;

	@Autowired
	private FoodCategoryRepository categoryRepository;

	@Autowired
	private FoodVariantRepository variantRepository;

	@Autowired
	private FoodAddonRepository addonRepository;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void contextLoads() {
	}

	@Test
	void seedsExpandedFoodMenu() {
		assertTrue(foodItemRepository.count() >= 25);
		assertTrue(categoryRepository.count() >= 5);
		assertTrue(foodItemRepository.findAllByFoodCategoryIsNull().isEmpty());
	}

	@Test
	void protectsMenuAndExposesItToAuthenticatedStudents() throws Exception {
		mockMvc.perform(get("/api/foods"))
				.andExpect(status().isUnauthorized());

		StudentUser student = new StudentUser(
				"Test Student",
				"test-" + java.util.UUID.randomUUID() + "@example.com",
				"+919876543210",
				"unused",
				"STUDENT");
		student.setEmailVerified(true);
		student = users.save(student);
		String token = jwtService.issue(student);

		mockMvc.perform(get("/api/foods").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
		mockMvc.perform(get("/api/foods/popular")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
		mockMvc.perform(get("/api/admin/orders")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/auth/logout")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/foods").header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void protectsPrivateOrdersFromAnonymousRequests() throws Exception {
		mockMvc.perform(get("/api/orders"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/admin/orders"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void registrationRequiresTermsAcceptance() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Test Student",
								  "phone": "+919876543210",
								  "email": "terms-test@example.com",
								  "password": "strong-password-123",
								  "termsAccepted": false
								}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void catalogSupportsStaffManagedProductsWithVariantsAndAddons() throws Exception {
		mockMvc.perform(get("/api/catalog/categories"))
				.andExpect(status().isUnauthorized());

		StudentUser admin = new StudentUser(
				"Catalog Admin",
				"catalog-admin-" + java.util.UUID.randomUUID() + "@example.com",
				"+919876543210",
				"unused",
				"ADMIN");
		admin = users.save(admin);
		String adminToken = jwtService.issue(admin);
		String categoryName = "Catalog Test " + java.util.UUID.randomUUID();

		mockMvc.perform(post("/api/admin/catalog/categories")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "%s",
								  "description": "Integration test category",
								  "displayOrder": 1,
								  "active": true
								}
								""".formatted(categoryName)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value(categoryName));

		Long categoryId = categoryRepository.findByNameIgnoreCase(categoryName)
				.orElseThrow().getId();
		String legacyCategoryName = "Legacy Admin Category " + java.util.UUID.randomUUID();
		mockMvc.perform(post("/api/admin/foods")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Legacy Admin Test Item",
								  "category": "%s",
								  "price": 15.25,
								  "imageUrl": "https://example.com/legacy.jpg",
								  "available": true
								}
								""".formatted(legacyCategoryName)))
				.andExpect(status().isCreated());
		FoodCategory legacyCategory = categoryRepository
				.findByNameIgnoreCase(legacyCategoryName).orElseThrow();
		assertTrue(foodItemRepository
				.countByFoodCategory_IdAndAvailableTrue(legacyCategory.getId()) == 1);

		String productName = "Test Paneer Product";
		var productResult = mockMvc.perform(post("/api/admin/catalog/products")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "%s",
								  "categoryId": %d,
								  "description": "A test product",
								  "basePrice": 123.45,
								  "imageUrl": "https://example.com/food.jpg",
								  "vegetarian": true,
								  "available": true,
								  "variants": [
								    {"name": "Large", "price": 145.50, "displayOrder": 1}
								  ],
								  "addons": [
								    {"name": "Extra cheese", "price": 12.25, "selectionGroup": "Extras"}
								  ]
								}
								""".formatted(productName, categoryId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.basePrice").value(123.45))
				.andExpect(jsonPath("$.variants[0].name").value("Large"))
				.andExpect(jsonPath("$.variants[0].price").value(145.50))
				.andExpect(jsonPath("$.addons[0].name").value("Extra cheese"))
				.andExpect(jsonPath("$.addons[0].price").value(12.25))
				.andReturn();

		Long productId = objectMapper.readTree(
				productResult.getResponse().getContentAsString()).path("id").asLong();
		var productJson = objectMapper.readTree(
				productResult.getResponse().getContentAsString());
		Long variantId = productJson.path("variants").get(0).path("id").asLong();
		Long addonId = productJson.path("addons").get(0).path("id").asLong();
		StudentUser student = new StudentUser(
				"Catalog Student",
				"catalog-student-" + java.util.UUID.randomUUID() + "@example.com",
				"+919876543210",
				"unused",
				"STUDENT");
		student = users.save(student);
		String studentToken = jwtService.issue(student);

		mockMvc.perform(get("/api/catalog/products/{id}", productId)
						.header("Authorization", "Bearer " + studentToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value(productName))
				.andExpect(jsonPath("$.basePrice").value(123.45))
				.andExpect(jsonPath("$.variants.length()").value(1))
				.andExpect(jsonPath("$.addons.length()").value(1));
		mockMvc.perform(get("/api/catalog/products")
						.param("categoryId", categoryId.toString())
						.header("Authorization", "Bearer " + studentToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value(productName))
				.andExpect(jsonPath("$[0].variants.length()").value(1))
				.andExpect(jsonPath("$[0].addons.length()").value(1));
		mockMvc.perform(post("/api/orders")
						.header("Authorization", "Bearer " + studentToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "studentName": "Catalog Student",
								  "studentPhone": "+919876543210",
								  "paymentMethod": "COUNTER",
								  "items": [{
								    "foodItemId": %d,
								    "quantity": 2,
								    "variantId": %d,
								    "addonIds": [%d]
								  }]
								}
								""".formatted(productId, variantId, addonId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.totalAmount").value(315.5))
				.andExpect(jsonPath("$.totalAmountDecimal").value(315.50))
				.andDo(result -> {
					Long orderId = objectMapper.readTree(
							result.getResponse().getContentAsString())
							.path("id").asLong();
					mockMvc.perform(get("/api/orders/{id}/items", orderId)
									.header("Authorization",
											"Bearer " + studentToken))
							.andExpect(status().isOk())
							.andExpect(jsonPath("$[0].itemPrice").value(157.75))
							.andExpect(jsonPath("$[0].unitPriceAmount").value(157.75))
							.andExpect(jsonPath("$[0].basePriceAmount").value(123.45))
							.andExpect(jsonPath("$[0].variantId").value(variantId))
							.andExpect(jsonPath("$[0].variantName").value("Large"))
							.andExpect(jsonPath("$[0].variantPriceAmount").value(145.50))
							.andExpect(jsonPath("$[0].addons[0].name").value("Extra cheese"))
							.andExpect(jsonPath("$[0].addons[0].price").value(12.25));
					FoodVariant variant = variantRepository.findById(variantId)
							.orElseThrow();
					variant.setName("Renamed large");
					variant.setPrice(new java.math.BigDecimal("999.99"));
					variantRepository.save(variant);
					FoodAddon addon = addonRepository.findById(addonId)
							.orElseThrow();
					addon.setName("Renamed cheese");
					addon.setPrice(new java.math.BigDecimal("50.00"));
					addonRepository.save(addon);
					mockMvc.perform(get("/api/orders/{id}/items", orderId)
									.header("Authorization",
											"Bearer " + studentToken))
							.andExpect(status().isOk())
							.andExpect(jsonPath("$[0].variantName").value("Large"))
							.andExpect(jsonPath("$[0].variantPriceAmount").value(145.50))
							.andExpect(jsonPath("$[0].addons[0].name").value("Extra cheese"))
							.andExpect(jsonPath("$[0].addons[0].price").value(12.25));
				});
		var secondProductResult = mockMvc.perform(
						post("/api/admin/catalog/products")
								.header("Authorization", "Bearer " + adminToken)
								.contentType(MediaType.APPLICATION_JSON)
								.content("""
										{
										  "name": "Other test product",
										  "categoryId": %d,
										  "basePrice": 20.00,
										  "available": true,
										  "variants": [],
										  "addons": []
										}
										""".formatted(categoryId)))
				.andExpect(status().isCreated())
				.andReturn();
		Long secondProductId = objectMapper.readTree(
				secondProductResult.getResponse().getContentAsString())
				.path("id").asLong();
		mockMvc.perform(post("/api/orders")
						.header("Authorization", "Bearer " + studentToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "studentName": "Catalog Student",
								  "studentPhone": "+919876543210",
								  "paymentMethod": "COUNTER",
								  "items": [{
								    "foodItemId": %d,
								    "quantity": 1,
								    "variantId": %d
								  }]
								}
								""".formatted(secondProductId, variantId)))
				.andExpect(status().isBadRequest())
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"Selected variant is unavailable")));
		mockMvc.perform(post("/api/orders")
						.header("Authorization", "Bearer " + studentToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "studentName": "Catalog Student",
								  "studentPhone": "+919876543210",
								  "paymentMethod": "COUNTER",
								  "items": [{
								    "foodItemId": %d,
								    "quantity": 1,
								    "addonIds": [%d]
								  }]
								}
								""".formatted(secondProductId, addonId)))
				.andExpect(status().isBadRequest())
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"Selected add-on is unavailable")));
		mockMvc.perform(post("/api/orders")
						.header("Authorization", "Bearer " + studentToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "studentName": "Catalog Student",
								  "studentPhone": "+919876543210",
								  "paymentMethod": "COUNTER",
								  "items": [{"foodItemId": %d, "quantity": 1}]
								}
								""".formatted(productId)))
				.andExpect(status().isBadRequest())
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"Choose a variant")));
		mockMvc.perform(post("/api/admin/catalog/categories")
						.header("Authorization", "Bearer " + studentToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Unauthorized test category"}
								"""))
				.andExpect(status().isForbidden());
	}

}
