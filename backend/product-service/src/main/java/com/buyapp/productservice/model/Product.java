package com.buyapp.productservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Document(collection = "products")
public class Product {
    @Id
    private String id;

    @NotBlank(message = "Product name can't be empty")
    @Size(min = 1, max = 50)
    private String name;

    @NotBlank(message = "Product description can't be empty")
    @Size(min = 2, max = 150)
    private String description;

    @Positive(message = "Product price can't be negative")
    private Double price;

    @Min(value = 0, message = "Quality must be between 0 and 100")
    @Max(value = 100, message = "Quality must be between 0 and 100")
    private Integer quality;

    @Field("userId")
    private String userId;

    public Product() {
    }

    public Product(String id, String name, String description, double price, Integer quality, String userId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.quality = quality;
        this.userId = userId;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getQuality() {
        return quality;
    }

    public void setQuality(Integer quality) {
        this.quality = quality;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}