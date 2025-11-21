package com.buyapp.common.test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * JWT test utility for generating tokens in integration tests.
 * Uses the same secret key as configured in application-test.yml
 */
public class JwtTestUtil {

    private static final String SECRET_KEY = "D341j5kfy5b3uFW/Xcw0KvP4/gfwho2UzAXAI6fGpUc=";
    private static final long JWT_EXPIRATION_MS = 3600000; // 1 hour for tests

    private static SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate a JWT token with email and role
     *
     * @param email User email
     * @param role  User role (CLIENT or SELLER)
     * @return JWT token string
     */
    public static String generateToken(String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role.toUpperCase());

        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + JWT_EXPIRATION_MS))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Generate a CLIENT token
     *
     * @param email User email
     * @return JWT token string
     */
    public static String generateClientToken(String email) {
        return generateToken(email, "CLIENT");
    }

    /**
     * Generate a SELLER token
     *
     * @param email User email
     * @return JWT token string
     */
    public static String generateSellerToken(String email) {
        return generateToken(email, "SELLER");
    }
}
