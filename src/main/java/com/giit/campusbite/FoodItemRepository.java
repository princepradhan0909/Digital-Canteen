
package com.giit.campusbite;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {

    List<FoodItem> findByCategoryIgnoreCase(String category);

    List<FoodItem> findByAvailableTrue();
}
