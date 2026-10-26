
package com.giit.campusbite;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "food_items", indexes = @Index(
        name = "idx_food_items_category_id",
        columnList = "food_category_id"))
public class FoodItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    @NotBlank
    private String category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_category_id")
    private FoodCategory foodCategory;

    @Size(max = 1000)
    @Column(length = 1000)
    private String description;

    private Boolean vegetarian;

    @NotNull
    @DecimalMin("0.0")
    private Double price;

    @DecimalMin("0.00")
    @Column(name = "price_amount", precision = 10, scale = 2)
    private BigDecimal priceAmount;

    private String imageUrl;

    private boolean available = true;

    public FoodItem() {
    }

    public FoodItem(String name, String category,
                    Double price, String imageUrl) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.priceAmount = BigDecimal.valueOf(price)
                .setScale(2, RoundingMode.HALF_UP);
        this.imageUrl = imageUrl;
        this.available = true;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public FoodCategory getFoodCategory() {
        return foodCategory;
    }

    public void setFoodCategory(FoodCategory foodCategory) {
        this.foodCategory = foodCategory;
        if (foodCategory != null) {
            this.category = foodCategory.getName();
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getVegetarian() {
        return vegetarian;
    }

    public void setVegetarian(Boolean vegetarian) {
        this.vegetarian = vegetarian;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
        this.priceAmount = price == null ? null : BigDecimal.valueOf(price)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getPriceAmount() {
        if (priceAmount != null) {
            return priceAmount;
        }
        return price == null ? null : BigDecimal.valueOf(price)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public void setPriceAmount(BigDecimal priceAmount) {
        this.priceAmount = priceAmount == null ? null
                : priceAmount.setScale(2, RoundingMode.UNNECESSARY);
        this.price = priceAmount == null ? null : priceAmount.doubleValue();
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
