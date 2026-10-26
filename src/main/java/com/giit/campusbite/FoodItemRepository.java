
package com.giit.campusbite;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {

    List<FoodItem> findByCategoryIgnoreCase(String category);

    List<FoodItem> findByAvailableTrue();

    boolean existsByNameIgnoreCase(String name);

    List<FoodItem> findAllByFoodCategoryIsNull();

    List<FoodItem> findAllByFoodCategory_Id(Long categoryId);

    List<FoodItem> findAllByFoodCategory_IdAndAvailableTrueOrderByNameAsc(Long categoryId);

    long countByFoodCategory_IdAndAvailableTrue(Long categoryId);
}
