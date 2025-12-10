package com.buyapp.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private TokenBlacklist tokenBlacklist;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Skip JWT processing for public endpoints (GET only)
        String requestPath = request.getRequestURI();
        String method = request.getMethod();
        if (isPublicEndpoint(requestPath, method)) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        String token = null;
        String username = null;

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
            if (tokenBlacklist.isBlacklisted(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token has been invalidated. Please log in again.");
                return; // Stop processing further
            }

            username = jwtUtil.extractUsername(token);
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (jwtUtil.isTokenValid(token, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails,
                        null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String path, String method) {
        // Auth endpoints (any method)
        if (path.equals("/auth/login") || path.equals("/auth/register")) {
            return true;
        }
        
        // Only GET requests can be public for the following endpoints
        if (!"GET".equalsIgnoreCase(method)) {
            return false;
        }
        
        // Public media endpoints (viewing images and avatars) - GET only
        if (path.startsWith("/media/file/") || path.startsWith("/media/product/") ||
            path.startsWith("/media/avatar/file/") || path.startsWith("/media/avatar/user/")) {
            return true;
        }
        // Public product endpoints (browsing products) - GET only
        if (path.equals("/products")) {
            return true;
        }
        // Single product by ID (e.g., /products/abc123) - GET only
        // but NOT /products/my-products or /products/user/...
        if (path.startsWith("/products/")) {
            String subPath = path.substring("/products/".length());
            // Only allow if it's a simple ID (no slashes, not "my-products", not "user")
            if (!subPath.contains("/") && !subPath.equals("my-products") && !subPath.startsWith("user")) {
                return true;
            }
        }
        // Actuator health checks - GET only
        if (path.startsWith("/actuator/")) {
            return true;
        }
        return false;
    }
}
