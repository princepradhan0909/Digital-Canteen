package com.giit.campusbite;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Entity
@Table(name = "food_addons", uniqueConstraints = @UniqueConstraint(
        name = "uk_food_addon_product_name",
        columnNames = {"food_item_id", "name"}))
public class FoodAddon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "food_item_id", nullable = false)
    private FoodItem foodItem;

    @NotBlank
    @Size(max = 80)
    @Column(nullable = false, length = 80)
    private String name;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 8, fraction = 2)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Size(max = 60)
    @Column(length = 60)
    private String selectionGroup;

    @Column(nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean available = true;

    protected FoodAddon() {
    }

    public FoodAddon(FoodItem foodItem, String name, BigDecimal price,
                     String selectionGroup, int displayOrder) {
        this.foodItem = foodItem;
        this.name = name;
        this.price = price;
        this.selectionGroup = selectionGroup;
        this.displayOrder = displayOrder;
        this.available = true;
    }

    public Long getId() { return id; }
    public FoodItem getFoodItem() { return foodItem; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public String getSelectionGroup() { return selectionGroup; }
    public int getDisplayOrder() { return displayOrder; }
    public boolean isAvailable() { return available; }
    public void setFoodItem(FoodItem foodItem) { this.foodItem = foodItem; }
    public void setName(String name) { this.name = name; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setSelectionGroup(String selectionGroup) { this.selectionGroup = selectionGroup; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public void setAvailable(boolean available) { this.available = available; }
}
