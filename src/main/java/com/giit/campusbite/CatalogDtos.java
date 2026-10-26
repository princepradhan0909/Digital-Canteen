package com.giit.campusbite;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record CategoryRequest(
            @NotBlank @Size(max = 80) String name,
            @Size(max = 500) String description,
            Integer displayOrder,
            Boolean active) {
    }

    public record CategoryResponse(
            Long id,
            String name,
            String description,
            int displayOrder,
            boolean active,
            long itemCount) {
    }

    public record ProductRequest(
            @NotBlank @Size(max = 100) String name,
            @NotNull Long categoryId,
            @Size(max = 1000) String description,
            @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2)
            BigDecimal basePrice,
            @Size(max = 1000) String imageUrl,
            Boolean vegetarian,
            @NotNull Boolean available,
            @Valid List<VariantRequest> variants,
            @Valid List<AddonRequest> addons) {
    }

    public record VariantRequest(
            Long id,
            @NotBlank @Size(max = 80) String name,
            @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2)
            BigDecimal price,
            Integer displayOrder,
            Boolean available) {
    }

    public record AddonRequest(
            Long id,
            @NotBlank @Size(max = 80) String name,
            @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2)
            BigDecimal price,
            @Size(max = 60) String selectionGroup,
            Integer displayOrder,
            Boolean available) {
    }

    public record VariantResponse(
            Long id, String name, BigDecimal price,
            int displayOrder, boolean available) {
    }

    public record AddonResponse(
            Long id, String name, BigDecimal price, String selectionGroup,
            int displayOrder, boolean available) {
    }

    public record ProductResponse(
            Long id,
            String name,
            Long categoryId,
            String category,
            String description,
            BigDecimal basePrice,
            String imageUrl,
            Boolean vegetarian,
            boolean available,
            List<VariantResponse> variants,
            List<AddonResponse> addons) {
    }
}
