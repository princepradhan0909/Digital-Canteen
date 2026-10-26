
package com.giit.campusbite;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/foods")
public class FoodItemController {

    private final FoodItemRepository foodItemRepository;
    private final OrderItemRepository orderItemRepository;

    public FoodItemController(FoodItemRepository foodItemRepository,
                              OrderItemRepository orderItemRepository) {
        this.foodItemRepository = foodItemRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @GetMapping("/popular")
    public ResponseEntity<List<FoodItem>> getPopularFoods() {
        List<FoodItem> popular = orderItemRepository.findPopularFoodIds().stream()
                .map(row -> foodItemRepository.findById((Long) row[0]).orElse(null))
                .filter(food -> food != null && food.isAvailable())
                .limit(8)
                .toList();
        return ResponseEntity.ok(popular);
    }

    @GetMapping
    public ResponseEntity<List<FoodItem>> getAllFoods() {
        return ResponseEntity.ok(foodItemRepository.findAll());
    }

    @GetMapping("/available")
    public ResponseEntity<List<FoodItem>> getAvailableFoods() {
        return ResponseEntity.ok(
            foodItemRepository.findByAvailableTrue()
        );
    }

    @GetMapping("/category")
    public ResponseEntity<List<FoodItem>> getFoodsByCategory(
            @org.springframework.web.bind.annotation.RequestParam String name) {

        return ResponseEntity.ok(
            foodItemRepository.findByCategoryIgnoreCase(name)
        );
    }
}
