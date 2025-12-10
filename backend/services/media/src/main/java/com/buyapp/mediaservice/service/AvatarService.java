package com.buyapp.mediaservice.service;

import com.buyapp.common.dto.UserDto;
import com.buyapp.common.exception.BadRequestException;
import com.buyapp.common.exception.ForbiddenException;
import com.buyapp.common.exception.ResourceNotFoundException;
import com.buyapp.mediaservice.model.Avatar;
import com.buyapp.mediaservice.repository.AvatarRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

@Service
public class AvatarService {

    @Autowired
    private AvatarRepository avatarRepository;

    @Autowired
    private WebClient.Builder webClientBuilder;

    private static final String AVATAR_UPLOAD_DIR = "uploads/avatars/";
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB
    private static final String[] ALLOWED_CONTENT_TYPES = {
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    };

    public AvatarService() {
        // Create upload directory if it doesn't exist
        try {
            Path uploadPath = Paths.get(AVATAR_UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not create avatar upload directory", e);
        }
    }

    @Transactional
    public Avatar uploadAvatar(MultipartFile file, Authentication authentication) {
        validateFile(file);

        // Get current user information from User Service
        String userEmail = authentication.getName();
        UserDto currentUser = getUserFromService(userEmail);
        if (currentUser == null) {
            throw new IllegalArgumentException("Authenticated user not found");
        }

        // Only sellers can have avatars
        if (!"seller".equalsIgnoreCase(currentUser.getRole())) {
            throw new ForbiddenException("Only sellers can upload avatars");
        }

        // Delete existing avatar if exists
        Optional<Avatar> existingAvatar = avatarRepository.findByUserId(currentUser.getId());
        if (existingAvatar.isPresent()) {
            deleteAvatarFile(existingAvatar.get().getImagePath());
            avatarRepository.delete(existingAvatar.get());
        }

        try {
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

            // Save file to disk
            Path targetLocation = Paths.get(AVATAR_UPLOAD_DIR + uniqueFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Create avatar entity
            Avatar avatar = new Avatar();
            avatar.setImagePath(AVATAR_UPLOAD_DIR + uniqueFilename);
            avatar.setUserId(currentUser.getId());
            avatar.setFileName(originalFilename);
            avatar.setContentType(file.getContentType());
            avatar.setFileSize(file.getSize());

            Avatar saved = avatarRepository.save(avatar);

            // Update user's avatar field in User Service
            updateUserAvatar(currentUser.getId(), saved.getId());

            return saved;
        } catch (IOException e) {
            throw new BadRequestException("Could not store avatar file: " + e.getMessage());
        }
    }

    public Avatar getAvatarByUserId(String userId) {
        return avatarRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Avatar not found for user: " + userId));
    }

    public Optional<Avatar> getAvatarByUserIdOptional(String userId) {
        return avatarRepository.findByUserId(userId);
    }

    public Avatar getAvatarById(String id) {
        return avatarRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Avatar not found with id: " + id));
    }

    @Transactional
    public void deleteAvatar(Authentication authentication) {
        String userEmail = authentication.getName();
        UserDto currentUser = getUserFromService(userEmail);
        if (currentUser == null) {
            throw new IllegalArgumentException("Authenticated user not found");
        }

        Optional<Avatar> avatar = avatarRepository.findByUserId(currentUser.getId());
        if (avatar.isEmpty()) {
            throw new ResourceNotFoundException("No avatar found for current user");
        }

        // Delete file from disk
        deleteAvatarFile(avatar.get().getImagePath());

        // Delete from database
        avatarRepository.delete(avatar.get());

        // Clear user's avatar field
        updateUserAvatar(currentUser.getId(), null);
    }

    private void deleteAvatarFile(String imagePath) {
        try {
            Path filePath = Paths.get(imagePath);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            System.err.println("Could not delete avatar file: " + imagePath + " - " + e.getMessage());
        }
    }

    private UserDto getUserFromService(String email) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri("http://user-service/users/email/{email}", email)
                    .retrieve()
                    .bodyToMono(UserDto.class)
                    .block();
        } catch (Exception e) {
            return null;
        }
    }

    private void updateUserAvatar(String userId, String avatarId) {
        try {
            webClientBuilder.build()
                    .put()
                    .uri("http://user-service/users/internal/avatar/{userId}", userId)
                    .contentType(org.springframework.http.MediaType.TEXT_PLAIN)
                    .bodyValue(avatarId != null ? avatarId : "")
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (Exception e) {
            System.err.println("Could not update user avatar: " + e.getMessage());
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("File cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds maximum limit of 2MB");
        }

        String contentType = file.getContentType();
        boolean isValidType = false;
        for (String allowedType : ALLOWED_CONTENT_TYPES) {
            if (allowedType.equals(contentType)) {
                isValidType = true;
                break;
            }
        }

        if (!isValidType) {
            throw new BadRequestException("Invalid file type. Only image files (JPEG, PNG, GIF, WebP) are allowed");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new BadRequestException("Invalid filename");
        }
    }
}
