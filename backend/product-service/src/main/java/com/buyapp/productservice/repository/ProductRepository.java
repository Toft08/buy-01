package com.buyapp.productservice.repository;

import com.buyapp.productservice.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findByUserId(String userId);

    void deleteByUserId(String userId);
}