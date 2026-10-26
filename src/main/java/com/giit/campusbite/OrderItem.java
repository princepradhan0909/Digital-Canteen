
package com.giit.campusbite;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private CustomerOrder customerOrder;

    @ManyToOne
    @JoinColumn(name = "food_item_id", nullable = false)
    private FoodItem foodItem;

    @Min(1)
    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Double itemPrice;

    @Column(name = "unit_price_amount", precision = 10, scale = 2)
    private BigDecimal unitPriceAmount;

    @Column(name = "base_price_amount", precision = 10, scale = 2)
    private BigDecimal basePriceAmount;

    @Column(name = "variant_id")
    private Long variantId;

    @Column(name = "variant_name", length = 80)
    private String variantName;

    @Column(name = "variant_price_amount", precision = 10, scale = 2)
    private BigDecimal variantPriceAmount;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_item_addons",
            joinColumns = @JoinColumn(name = "order_item_id"))
    @OrderColumn(name = "display_order")
    private List<AddonSnapshot> addons = new ArrayList<>();

    public OrderItem() {
    }

    public OrderItem(CustomerOrder customerOrder, FoodItem foodItem,
                     Integer quantity, Double itemPrice) {
        this.customerOrder = customerOrder;
        this.foodItem = foodItem;
        this.quantity = quantity;
        this.itemPrice = itemPrice;
        this.unitPriceAmount = itemPrice == null ? null
                : BigDecimal.valueOf(itemPrice).setScale(2, RoundingMode.HALF_UP);
        this.basePriceAmount = this.unitPriceAmount;
    }

    public Long getId() {
        return id;
    }

    public CustomerOrder getCustomerOrder() {
        return customerOrder;
    }

    public void setCustomerOrder(CustomerOrder customerOrder) {
        this.customerOrder = customerOrder;
    }

    public FoodItem getFoodItem() {
        return foodItem;
    }

    public void setFoodItem(FoodItem foodItem) {
        this.foodItem = foodItem;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Double getItemPrice() {
        return itemPrice;
    }

    public void setItemPrice(Double itemPrice) {
        this.itemPrice = itemPrice;
        this.unitPriceAmount = itemPrice == null ? null
                : BigDecimal.valueOf(itemPrice).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getUnitPriceAmount() {
        if (unitPriceAmount != null) {
            return unitPriceAmount;
        }
        return itemPrice == null ? null
                : BigDecimal.valueOf(itemPrice).setScale(2, RoundingMode.HALF_UP);
    }

    public void setUnitPriceAmount(BigDecimal unitPriceAmount) {
        this.unitPriceAmount = unitPriceAmount == null ? null
                : unitPriceAmount.setScale(2, RoundingMode.UNNECESSARY);
        this.itemPrice = unitPriceAmount == null
                ? null : unitPriceAmount.doubleValue();
    }

    public BigDecimal getBasePriceAmount() {
        return basePriceAmount == null ? getUnitPriceAmount() : basePriceAmount;
    }

    public void setBasePriceAmount(BigDecimal basePriceAmount) {
        this.basePriceAmount = basePriceAmount == null ? null
                : basePriceAmount.setScale(2, RoundingMode.UNNECESSARY);
    }

    public Long getVariantId() {
        return variantId;
    }

    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }

    public String getVariantName() {
        return variantName;
    }

    public void setVariantName(String variantName) {
        this.variantName = variantName;
    }

    public BigDecimal getVariantPriceAmount() {
        return variantPriceAmount;
    }

    public void setVariantPriceAmount(BigDecimal variantPriceAmount) {
        this.variantPriceAmount = variantPriceAmount == null ? null
                : variantPriceAmount.setScale(2, RoundingMode.UNNECESSARY);
    }

    public List<AddonSnapshot> getAddons() {
        return addons;
    }

    public void setAddons(List<AddonSnapshot> addons) {
        this.addons = addons == null ? new ArrayList<>() : new ArrayList<>(addons);
    }

    @Embeddable
    public static class AddonSnapshot {

        @Column(name = "catalog_addon_id")
        private Long catalogAddonId;

        @Column(name = "addon_name", nullable = false, length = 80)
        private String name;

        @Column(name = "addon_price", nullable = false, precision = 10, scale = 2)
        private BigDecimal price;

        protected AddonSnapshot() {
        }

        public AddonSnapshot(Long catalogAddonId, String name, BigDecimal price) {
            this.catalogAddonId = catalogAddonId;
            this.name = name;
            this.price = price.setScale(2, RoundingMode.UNNECESSARY);
        }

        public Long getCatalogAddonId() {
            return catalogAddonId;
        }

        public String getName() {
            return name;
        }

        public BigDecimal getPrice() {
            return price;
        }
    }
}
