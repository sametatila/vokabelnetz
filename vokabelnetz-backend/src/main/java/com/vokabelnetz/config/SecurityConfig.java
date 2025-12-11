package com.vokabelnetz.config;

import com.vokabelnetz.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security configuration.
 * Based on SECURITY.md documentation.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsProperties corsProperties;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/health").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/swagger-ui.html").permitAll()

                // Word endpoints - authenticated users
                .requestMatchers(HttpMethod.GET, "/words/**").hasAnyRole("USER", "ADMIN", "SUPER")

                // Learning endpoints - authenticated users
                .requestMatchers("/learning/**").hasAnyRole("USER", "ADMIN", "SUPER")

                // Progress endpoints - authenticated users
                .requestMatchers("/progress/**").hasAnyRole("USER", "ADMIN", "SUPER")

                // User endpoints
                .requestMatchers("/users/me/**").hasAnyRole("USER", "ADMIN", "SUPER")

                // Admin endpoints
                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER")

                // Super admin endpoints
                .requestMatchers("/admin/users/*/role").hasRole("SUPER")
                .requestMatchers("/admin/system/**").hasRole("SUPER")

                // Default: require authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(
            Arrays.asList(corsProperties.getAllowedOrigins().split(","))
        );
        configuration.setAllowedMethods(
            Arrays.asList(corsProperties.getAllowedMethods().split(","))
        );
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(corsProperties.isAllowCredentials());
        configuration.setMaxAge(corsProperties.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Password encoder using Argon2id.
     * Based on SECURITY.md - Argon2id is the recommended algorithm.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // Argon2id configuration:
        // - Salt length: 16 bytes
        // - Hash length: 32 bytes
        // - Parallelism: 1
        // - Memory: 16 MB (16384 KB)
        // - Iterations: 2
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    public AuthenticationManager authenticationManager(
        AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }
}
