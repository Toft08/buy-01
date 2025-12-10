package com.buyapp.mediaservice.controller;

import com.buyapp.mediaservice.model.Avatar;
import com.buyapp.mediaservice.service.AvatarService;
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
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/media/avatar")
public class AvatarController {

    @Autowired
    private AvatarService avatarService;

    /**
     * Upload avatar for authenticated seller
     */
    @PostMapping("/upload")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Avatar> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        Avatar savedAvatar = avatarService.uploadAvatar(file, authentication);
        return ResponseEntity.ok(savedAvatar);
    }

    /**
     * Get avatar metadata by user ID (public endpoint)
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Avatar> getAvatarByUserId(@PathVariable String userId) {
        Optional<Avatar> avatar = avatarService.getAvatarByUserIdOptional(userId);
        return avatar.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get avatar image file by avatar ID (public endpoint)
     */
    @GetMapping("/file/{id}")
    public ResponseEntity<Resource> getAvatarFile(@PathVariable String id) {
        try {
            Avatar avatar = avatarService.getAvatarById(id);
            Path filePath = Paths.get(avatar.getImagePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + avatar.getFileName() + "\"")
                        .body(resource);
            } else {
                throw new RuntimeException("File not found: " + avatar.getFileName());
            }
        } catch (IOException e) {
            throw new RuntimeException("File not found");
        }
    }

    /**
     * Delete avatar for authenticated seller
     */
    @DeleteMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Map<String, String>> deleteAvatar(Authentication authentication) {
        avatarService.deleteAvatar(authentication);
        return ResponseEntity.ok(Map.of("message", "Avatar deleted successfully"));
    }
}
