package com.buyapp.mediaservice.config;

import java.util.ArrayList;
import java.util.Collection;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class JwtUserDetailsService implements UserDetailsService {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Get the JWT token from the current request
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new UsernameNotFoundException("No request context available");
        }

        HttpServletRequest request = attributes.getRequest();
        String token = null;
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
        }

        if (token == null) {
            throw new UsernameNotFoundException("No JWT token found");
        }

        try {
            // Parse the JWT to extract claims
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String email = claims.getSubject();
            String role = claims.get("role", String.class);

            // Extract roles from claims
            Collection<GrantedAuthority> authorities = new ArrayList<>();

            if (role != null) {
                // Add ROLE_ prefix if not already present
                if (!role.startsWith("ROLE_")) {
                    role = "ROLE_" + role;
                }
                authorities.add(new SimpleGrantedAuthority(role));
            } else {
                // Fallback if no role in token
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            }

            return User.builder()
                    .username(email)
                    .password("") // Password not needed for JWT auth
                    .authorities(authorities)
                    .build();
        } catch (Exception e) {
            throw new UsernameNotFoundException("Invalid JWT token: " + e.getMessage());
        }
    }
}
