package com.restconcepts;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class RestParametersDemo {

    // =========================================================
    // 1. PATH VARIABLE (@PathVariable)
    // "Identify a specific resource"
    // =========================================================
    // URL: GET /api/products/101
    @GetMapping("/{id}")
    public String getProductById(@PathVariable("id") Long id) {
        return "Fetching specific product with ID: " + id;
    }

    // URL: GET /api/products/101/reviews/5
    @GetMapping("/{productId}/reviews/{reviewId}")
    public String getReview(@PathVariable Long productId, @PathVariable Long reviewId) {
        return "Fetching Review #" + reviewId + " for Product #" + productId;
    }

    // =========================================================
    // 2. QUERY PARAMETERS (@RequestParam)
    // "Filter, Sort, or Search resources"
    // =========================================================
    // URL: GET /api/products?category=electronics&price=500
    // URL: GET /api/products?category=books (price is optional)
    @GetMapping
    public String searchProducts(
            @RequestParam("category") String category,
            @RequestParam(value = "price", required = false, defaultValue = "0") Double maxPrice) {

        return "Searching products in Category: " + category + " with Price < " + maxPrice;
    }

    // =========================================================
    // 3. REQUEST BODY (@RequestBody)
    // "Create or Update complex data"
    // =========================================================
    // URL: POST /api/products
    // Body: { "name": "Laptop", "price": 1200.00 }
    @PostMapping
    public String createProduct(@RequestBody ProductDTO product) {
        return "Creating Product: " + product;
    }

    // URL: PUT /api/products/101
    // Body: { "name": "Gaming Laptop", "price": 1500.00 }
    @PutMapping("/{id}")
    public String updateProduct(@PathVariable Long id, @RequestBody ProductDTO product) {
        return "Updating Product #" + id + " with new data: " + product;
    }
}

// Simple DTO
class ProductDTO {
    public String name;
    public double price;

    @Override
    public String toString() {
        return "Name=" + name + ", Price=$" + price;
    }
}

