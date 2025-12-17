package com.buyapp.apigateway.filter;

import com.buyapp.apigateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class JwtRequestFilter implements WebFilter {

    @Autowired
    private JwtUtil jwtUtil;

    // Public endpoints that don't require authentication
    private static final List<String> EXCLUDE_URLS = Arrays.asList(
            "/auth/",
            "/actuator/health");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();

        // Allow public endpoints
        if (isPublicEndpoint(path, method)) {
            return chain.filter(exchange);
        }

        // Extract Authorization header
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            String token = authHeader.substring(7);
            Claims claims = jwtUtil.extractClaims(token);

            String email = claims.getSubject();
            String role = claims.get("role", String.class);

            // Create authentication token with role
            List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(email, null,
                    authorities);

            // Add user info as headers for downstream services
            ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                    .header("X-User-Email", email)
                    .header("X-User-Role", role)
                    .build();

            ServerWebExchange modifiedExchange = exchange.mutate()
                    .request(modifiedRequest)
                    .build();

            // Set security context and continue
            return chain.filter(modifiedExchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));

        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private boolean isPublicEndpoint(String path, String method) {
        // Public auth endpoints
        if (path.startsWith("/auth/")) {
            return true;
        }

        // Public GET endpoints for products and media
        if ("GET".equalsIgnoreCase(method)) {
            if (path.startsWith("/products") || path.startsWith("/media/")) {
                return true;
            }
        }

        // Health check
        if (path.startsWith("/actuator/health")) {
            return true;
        }

        return false;
    }
}
