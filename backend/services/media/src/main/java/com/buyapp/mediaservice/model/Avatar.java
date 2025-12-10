package com.buyapp.mediaservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;

@Document(collection = "avatars")
public class Avatar {
    @Id
    private String id;

    @NotBlank(message = "Image path can't be empty")
    private String imagePath;

    @NotBlank(message = "User ID can't be null")
    @Indexed(unique = true)
    private String userId;

    private String fileName;
    private String contentType;
    private Long fileSize;

    public Avatar() {
    }

    public Avatar(String id, String imagePath, String userId, String fileName, String contentType, Long fileSize) {
        this.id = id;
        this.imagePath = imagePath;
        this.userId = userId;
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

