package com.buyapp.productservice.controller;

import com.buyapp.common.dto.ProductDto;
import com.buyapp.common.test.JwtTestUtil;
import com.buyapp.productservice.model.Product;
import com.buyapp.productservice.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductControllerIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:latest")
            .withExposedPorts(27017);

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    private static String sellerToken;
    private static String clientToken;
    private static String productId;

    @BeforeAll
    static void setupTokens() {
        // Generate real JWT tokens using test utility
        sellerToken = JwtTestUtil.generateSellerToken("seller@test.com");
        clientToken = JwtTestUtil.generateClientToken("client@test.com");
    }

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("Should create product when user is SELLER")
    void testCreateProductAsSeller() throws Exception {
        ProductDto productDto = new ProductDto();
        productDto.setName("Gaming Laptop");
        productDto.setDescription("High-end gaming laptop");
        productDto.setPrice(1299.99);
        productDto.setQuality(5);

        MvcResult result = mockMvc.perform(post("/products")
                .header("Authorization", "Bearer " + sellerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Gaming Laptop"))
                .andExpect(jsonPath("$.price").value(1299.99))
                .andExpect(jsonPath("$.quality").value(5))
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        productId = objectMapper.readTree(response).get("id").asText();
    }

    @Test
    @Order(2)
    @DisplayName("Should reject product creation when user is CLIENT")
    void testCreateProductAsClient() throws Exception {
        ProductDto productDto = new ProductDto();
        productDto.setName("Unauthorized Product");
        productDto.setDescription("Should not be created");
        productDto.setPrice(99.99);
        productDto.setQuality(3);

        mockMvc.perform(post("/products")
                .header("Authorization", "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(3)
    @DisplayName("Should get all products without authentication")
    void testGetAllProducts() throws Exception {
        // Create test products
        Product product1 = new Product();
        product1.setName("Product 1");
        product1.setDescription("Description 1");
        product1.setPrice(100.0);
        product1.setQuality(4);
        product1.setUserId("seller-id");
        productRepository.save(product1);

        Product product2 = new Product();
        product2.setName("Product 2");
        product2.setDescription("Description 2");
        product2.setPrice(200.0);
        product2.setQuality(5);
        product2.setUserId("seller-id");
        productRepository.save(product2);

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[1].name").exists());
    }

    @Test
    @Order(4)
    @DisplayName("Should get product by ID without authentication")
    void testGetProductById() throws Exception {
        Product product = new Product();
        product.setName("Single Product");
        product.setDescription("Test product");
        product.setPrice(150.0);
        product.setQuality(4);
        product.setUserId("seller-id");
        product = productRepository.save(product);

        mockMvc.perform(get("/products/" + product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.getId()))
                .andExpect(jsonPath("$.name").value("Single Product"))
                .andExpect(jsonPath("$.price").value(150.0));
    }

    @Test
    @Order(5)
    @DisplayName("Should return 404 for non-existent product")
    void testGetNonExistentProduct() throws Exception {
        mockMvc.perform(get("/products/nonexistent123456"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("not found")));
    }

    @Test
    @Order(6)
    @DisplayName("Should update product when owner")
    void testUpdateProductAsOwner() throws Exception {
        Product product = new Product();
        product.setName("Original Name");
        product.setDescription("Original description");
        product.setPrice(100.0);
        product.setQuality(3);
        product.setUserId("seller-user-id");
        product = productRepository.save(product);

        ProductDto updateDto = new ProductDto();
        updateDto.setName("Updated Name");
        updateDto.setDescription("Updated description");
        updateDto.setPrice(150.0);
        updateDto.setQuality(4);

        mockMvc.perform(put("/products/" + product.getId())
                .header("Authorization", "Bearer " + sellerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.price").value(150.0));
    }

    @Test
    @Order(7)
    @DisplayName("Should reject update when not owner")
    void testUpdateProductAsNonOwner() throws Exception {
        Product product = new Product();
        product.setName("Seller Product");
        product.setDescription("Owned by seller");
        product.setPrice(100.0);
        product.setQuality(3);
        product.setUserId("seller-user-id");
        product = productRepository.save(product);

        ProductDto updateDto = new ProductDto();
        updateDto.setName("Hacked Name");
        updateDto.setDescription("Hacked");
        updateDto.setPrice(1.0);
        updateDto.setQuality(1);

        mockMvc.perform(put("/products/" + product.getId())
                .header("Authorization", "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(8)
    @DisplayName("Should delete product when owner")
    void testDeleteProductAsOwner() throws Exception {
        Product product = new Product();
        product.setName("To Delete");
        product.setDescription("Will be deleted");
        product.setPrice(50.0);
        product.setQuality(2);
        product.setUserId("seller-user-id");
        product = productRepository.save(product);

        mockMvc.perform(delete("/products/" + product.getId())
                .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk());

        // Verify deletion
        mockMvc.perform(get("/products/" + product.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(9)
    @DisplayName("Should reject product with missing required fields")
    void testCreateProductMissingFields() throws Exception {
        ProductDto productDto = new ProductDto();
        // Missing name
        productDto.setDescription("Missing name field");
        productDto.setPrice(99.99);

        mockMvc.perform(post("/products")
                .header("Authorization", "Bearer " + sellerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(10)
    @DisplayName("Should reject product with negative price")
    void testCreateProductNegativePrice() throws Exception {
        ProductDto productDto = new ProductDto();
        productDto.setName("Invalid Product");
        productDto.setDescription("Has negative price");
        productDto.setPrice(-10.0);
        productDto.setQuality(3);

        mockMvc.perform(post("/products")
                .header("Authorization", "Bearer " + sellerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productDto)))
                .andExpect(status().isBadRequest());
    }
}
