package com.buyapp.userservice.controller;

import com.buyapp.common.dto.UserDto;
import com.buyapp.common.exception.ResourceNotFoundException;
import com.buyapp.userservice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserDto> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public UserDto getUserById(@PathVariable String id) {
        // This endpoint is for internal service calls, so no strict authorization
        return userService.getUserById(id);
    }

    @GetMapping("/email/{email}")
    public UserDto getUserByEmail(@PathVariable String email) {
        // This endpoint is for internal service calls, so no strict authorization
        return userService.getUserByEmail(email);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public UserDto getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        return userService.getUserByEmail(email);
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public UserDto updateCurrentUser(@Valid @RequestBody UserDto userDto, Authentication authentication) {
        String email = authentication.getName();
        UserDto currentUser = userService.getUserByEmail(email);
        return userService.updateUser(currentUser.getId(), userDto, authentication);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto createUser(@Valid @RequestBody UserDto userDto) {
        return userService.createUser(userDto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto updateUser(@PathVariable String id, @Valid @RequestBody UserDto userDto,
            Authentication authentication) {
        return userService.updateUser(id, userDto, authentication);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(@PathVariable String id, @AuthenticationPrincipal UserDetails userDetails) {
        if (!userService.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id:" + id);
        }

        userService.deleteUser(id, userDetails);
    }

    @DeleteMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public void deleteCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        UserDto user = userService.getUserByEmail(email);
        userService.deleteUser(user.getId(), userDetails);
    }

    /**
     * Internal endpoint for Media Service to update user's avatar field
     * Path: /users/internal/avatar/{id}
     */
    @PutMapping(value = "/internal/avatar/{id}", consumes = org.springframework.http.MediaType.TEXT_PLAIN_VALUE)
    public void updateUserAvatar(@PathVariable String id, @RequestBody(required = false) String avatarId) {
        userService.updateUserAvatar(id, avatarId);
    }
}
