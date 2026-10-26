package com.giit.campusbite;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FoodCategoryRepository extends JpaRepository<FoodCategory, Long> {

    Optional<FoodCategory> findByNameIgnoreCase(String name);

    List<FoodCategory> findAllByActiveTrueOrderByDisplayOrderAscNameAsc();

    List<FoodCategory> findAllByOrderByDisplayOrderAscNameAsc();
}
