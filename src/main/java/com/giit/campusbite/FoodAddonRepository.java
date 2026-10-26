package com.giit.campusbite;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodAddonRepository extends JpaRepository<FoodAddon, Long> {

    List<FoodAddon> findAllByFoodItem_IdOrderByDisplayOrderAscNameAsc(Long foodItemId);

    List<FoodAddon> findAllByFoodItem_IdInOrderByFoodItem_IdAscDisplayOrderAscNameAsc(
            List<Long> foodItemIds);

    List<FoodAddon> findAllByFoodItem_IdAndAvailableTrueOrderByDisplayOrderAscNameAsc(
            Long foodItemId);

    void deleteAllByFoodItem_Id(Long foodItemId);
}
