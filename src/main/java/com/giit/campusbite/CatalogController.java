package com.giit.campusbite;

import com.giit.campusbite.CatalogDtos.CategoryResponse;
import com.giit.campusbite.CatalogDtos.ProductResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> categories() {
        return ResponseEntity.ok(catalog.getCategories(false));
    }

    @GetMapping("/products")
    public ResponseEntity<List<ProductResponse>> products(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(catalog.getProducts(categoryId, search, false));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ProductResponse> product(@PathVariable Long id) {
        return ResponseEntity.ok(catalog.getProduct(id));
    }
}
