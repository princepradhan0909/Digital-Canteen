
package com.giit.campusbite;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class FoodDataLoader {

    @Bean
    CommandLineRunner loadFoodData(FoodItemRepository repository) {
        return args -> {

            if (repository.count() > 0) {
                return;
            }

            List<FoodItem> foods = List.of(
                new FoodItem("Veg Burger", "Snacks", 60.0, "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=600"),
                new FoodItem("Cheese Pizza", "Fast Food", 150.0, "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=600"),
                new FoodItem("Masala Dosa", "South Indian", 80.0, "https://images.unsplash.com/photo-1668236543090-82eba5ee5976?w=600"),
                new FoodItem("Veg Sandwich", "Snacks", 50.0, "https://images.unsplash.com/photo-1528735602780-2552fd46c7af?w=600"),
                new FoodItem("French Fries", "Snacks", 70.0, "https://images.unsplash.com/photo-1573080496219-bb080dd4f877?w=600"),
                new FoodItem("Chole Bhature", "North Indian", 100.0, "https://images.unsplash.com/photo-1626132647523-66f5bf380027?w=600"),
                new FoodItem("Veg Momos", "Snacks", 80.0, "https://images.unsplash.com/photo-1563245372-f21724e3856d?w=600"),
                new FoodItem("Paneer Roll", "Fast Food", 90.0, "https://images.unsplash.com/photo-1626700051175-6818013e1d4f?w=600"),
                new FoodItem("Veg Biryani", "Meals", 120.0, "https://images.unsplash.com/photo-1589302168068-964664d93dc0?w=600"),
                new FoodItem("Pasta", "Fast Food", 110.0, "https://images.unsplash.com/photo-1473093295043-cdd812d0e601?w=600"),
                new FoodItem("Cold Coffee", "Drinks", 70.0, "https://images.unsplash.com/photo-1461023058943-07fcbe16d735?w=600"),
                new FoodItem("Lemon Soda", "Drinks", 40.0, "https://images.unsplash.com/photo-1513558161293-cdaf765edfd7?w=600"),
                new FoodItem("Chocolate Brownie", "Desserts", 80.0, "https://images.unsplash.com/photo-1606313564200-e75d5e30476c?w=600"),
                new FoodItem("Gulab Jamun", "Desserts", 40.0, "https://images.unsplash.com/photo-1666190092159-4b1a1f1e1f8e?w=600"),
                new FoodItem("Veg Thali", "Meals", 140.0, "https://images.unsplash.com/photo-1546833999-b9f581a1996d?w=600")
            );

            repository.saveAll(foods);
            System.out.println("CampusBite: 15 food items inserted.");
        };
    }
}

