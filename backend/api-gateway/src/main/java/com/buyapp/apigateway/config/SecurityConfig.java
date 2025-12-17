package com.buyapp.apigateway.config;

import com.buyapp.apigateway.filter.JwtRequestFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeExchange(exchange -> exchange
                        // Public endpoints
                        .pathMatchers("/actuator/health").permitAll()
                        .pathMatchers("/auth/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/products/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/media/**").permitAll()

                        // Protected endpoints - user profile
                        .pathMatchers("/users/me").hasAnyRole("CLIENT", "SELLER")

                        // Protected endpoints - seller only
                        .pathMatchers(HttpMethod.POST, "/products/**").hasRole("SELLER")
                        .pathMatchers(HttpMethod.PUT, "/products/**").hasRole("SELLER")
                        .pathMatchers(HttpMethod.DELETE, "/products/**").hasRole("SELLER")
                        .pathMatchers(HttpMethod.POST, "/media/**").hasRole("SELLER")
                        .pathMatchers(HttpMethod.DELETE, "/media/**").hasRole("SELLER")

                        // All other endpoints require authentication
                        .anyExchange().authenticated())
                .addFilterAt(jwtRequestFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:4200", "http://localhost:8080"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
