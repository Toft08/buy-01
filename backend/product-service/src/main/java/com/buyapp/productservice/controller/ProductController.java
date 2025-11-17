package com.buyapp.productservice.controller;

import com.buyapp.common.dto.ProductDto;
import com.buyapp.productservice.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    // Public endpoints (no authentication required)
    @GetMapping
    public List<ProductDto> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/{id}")
    public ProductDto getProductById(@PathVariable String id) {
        return productService.getProductById(id);
    }

    // Internal endpoint for other services
    @GetMapping("/user/{userId}")
    public List<ProductDto> getProductsByUserId(@PathVariable String userId) {
        return productService.getProductsByUserId(userId);
    }

    // Protected endpoints (authentication required)
    @PostMapping
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ProductDto createProduct(@Valid @RequestBody ProductDto productDto, Authentication authentication) {
        return productService.createProduct(productDto, authentication);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ProductDto updateProduct(@PathVariable String id, @Valid @RequestBody ProductDto productDto,
            Authentication authentication) {
        return productService.updateProduct(id, productDto, authentication);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public void deleteProduct(@PathVariable String id, Authentication authentication) {
        productService.deleteProduct(id, authentication);
    }

    @GetMapping("/my-products")
    @PreAuthorize("isAuthenticated()")
    public List<ProductDto> getMyProducts(Authentication authentication) {
        String userEmail = authentication.getName();
        return productService.getProductsByUser(userEmail);
    }

    // Internal endpoint for user deletion (called by User Service)
    @DeleteMapping("/user/{userId}")
    public void deleteProductsByUserId(@PathVariable String userId) {
        productService.deleteProductsByUserId(userId);
    }
}