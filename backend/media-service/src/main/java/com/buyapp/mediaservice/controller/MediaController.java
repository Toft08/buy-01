package com.buyapp.mediaservice.controller;

import com.buyapp.mediaservice.model.Media;
import com.buyapp.mediaservice.service.MediaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/media")
public class MediaController {

    @Autowired
    private MediaService mediaService;

    @PostMapping("/upload/{productId}")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<Media> uploadMedia(
            @PathVariable String productId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        Media savedMedia = mediaService.uploadMedia(file, productId, authentication);
        return ResponseEntity.ok(savedMedia);
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<Media>> getMediaByProduct(@PathVariable String productId) {
        List<Media> mediaList = mediaService.getMediaByProductId(productId);
        return ResponseEntity.ok(mediaList);
    }

    @GetMapping("/file/{id}")
    public ResponseEntity<Resource> getMediaFile(@PathVariable String id) {
        try {
            Media media = mediaService.getMediaById(id);
            Path filePath = Paths.get(media.getImagePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + media.getFileName() + "\"")
                        .body(resource);
            } else {
                throw new RuntimeException("File not found: " + media.getFileName());
            }
        } catch (IOException e) {
            throw new RuntimeException("File not found");
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteMedia(
            @PathVariable String id,
            Authentication authentication) {

        mediaService.deleteMedia(id, authentication);
        return ResponseEntity.ok(Map.of("message", "Media deleted successfully"));
    }

    @DeleteMapping("/product/{productId}")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteMediaByProduct(
            @PathVariable String productId,
            Authentication authentication) {

        mediaService.deleteMediaByProductId(productId, authentication);
        return ResponseEntity.ok(Map.of("message", "All media for product deleted successfully"));
    }

    // Internal endpoint for other services (Product Service calls this when
    // deleting a product)
    @DeleteMapping("/internal/product/{productId}")
    public ResponseEntity<Map<String, String>> deleteMediaByProductInternal(
            @PathVariable String productId) {

        mediaService.deleteMediaByProductIdInternal(productId);
        return ResponseEntity.ok(Map.of("message", "All media for product deleted successfully"));
    }
}