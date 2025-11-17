package com.buyapp.userservice.controller;

import com.buyapp.common.dto.UserDto;
import com.buyapp.common.exception.UnauthorizedException;
import com.buyapp.userservice.model.User;
import com.buyapp.userservice.repository.UserRepository;
import com.buyapp.common.security.JwtUtil;
import com.buyapp.common.security.TokenBlacklist;
import com.buyapp.userservice.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenBlacklist tokenBlacklist;

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> loginData) {
        String email = loginData.get("email");
        String password = loginData.get("password");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());

        // Create HttpOnly cookie for the JWT token
        ResponseCookie jwtCookie = ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .secure(false) // Set to false for development
                .sameSite("Strict")
                .maxAge(24 * 60 * 60) // 24 hours
                .path("/")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(Map.of("token", token, "message", "Login successful"));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody UserDto userDto) {
        // Set default role to CLIENT if not specified
        if (userDto.getRole() == null || userDto.getRole().isEmpty()) {
            userDto.setRole("client");
        }

        // Validate role (only client or seller allowed)
        String role = userDto.getRole().toLowerCase();
        if (!role.equals("client") && !role.equals("seller")) {
            userDto.setRole("client"); // Default to client for invalid roles
        }

        UserDto createdUser = userService.createUser(userDto);
        String token = jwtUtil.generateToken(createdUser.getEmail(), createdUser.getRole());

        // Create HttpOnly cookie for the JWT token
        ResponseCookie jwtCookie = ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .secure(false) // Set to false for development
                .sameSite("Strict")
                .maxAge(24 * 60 * 60) // 24 hours
                .path("/")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(Map.of(
                        "user", createdUser,
                        "token", token,
                        "message", "User registered successfully"));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request,
            @RequestHeader(value = "Authorization", required = false) String header) {
        String token = null;

        // Try to get token from Authorization header first
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
        } else {
            // Try to get token from cookie
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("jwt".equals(cookie.getName())) {
                        token = cookie.getValue();
                        break;
                    }
                }
            }
        }

        if (token != null) {
            Date expiry = jwtUtil.extractExpiration(token);
            tokenBlacklist.blacklistToken(token, expiry);
        }

        // Clear the JWT cookie
        ResponseCookie clearCookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(false) // Set to false for development
                .sameSite("Strict")
                .maxAge(0) // Expire immediately
                .path("/")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .body("Logged out successfully.");
    }
}