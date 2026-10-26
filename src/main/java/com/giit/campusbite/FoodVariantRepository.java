package com.giit.campusbite;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodVariantRepository extends JpaRepository<FoodVariant, Long> {

    List<FoodVariant> findAllByFoodItem_IdOrderByDisplayOrderAscNameAsc(Long foodItemId);

    List<FoodVariant> findAllByFoodItem_IdInOrderByFoodItem_IdAscDisplayOrderAscNameAsc(
            List<Long> foodItemIds);

    List<FoodVariant> findAllByFoodItem_IdAndAvailableTrueOrderByDisplayOrderAscNameAsc(
            Long foodItemId);

    void deleteAllByFoodItem_Id(Long foodItemId);
}
