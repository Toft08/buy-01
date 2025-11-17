package com.buyapp.mediaservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Document(collection = "media")
public class Media {
    @Id
    private String id;

    @NotBlank(message = "Image path can't be empty")
    private String imagePath;

    @NotNull(message = "Product ID can't be null")
    @Field("productId")
    private String productId;

    private String fileName;
    private String contentType;
    private Long fileSize;

    public Media() {
    }

    public Media(String id, String imagePath, String productId, String fileName, String contentType, Long fileSize) {
        this.id = id;
        this.imagePath = imagePath;
        this.productId = productId;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
}