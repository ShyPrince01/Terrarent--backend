package com.terrarent.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity // Enable @PreAuthorize, @PostAuthorize, @Secured annotations
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;


//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Enable CORS
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF for stateless API
                .headers(headers -> headers
                    .frameOptions(frame -> frame.sameOrigin()) // ✅ REQUIRED for H2
                )
                .authorizeHttpRequests(auth -> auth

                       // ✅ H2 Console (DEV ONLY)
            .requestMatchers("/h2-console/**").permitAll() 
                        // Public endpoints
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/properties", // Public property search
                                "/api/properties/{id}", // Public view single property
                                "/api/amenities", // Public view amenities
                                "/api/booking-com/**", // Booking.com API
                                "/v2/api-docs",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-resources",
                                "/swagger-resources/**",
                                "/configuration/ui",
                                "/configuration/security",
                                "/swagger-ui/**",
                                "/webjars/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // Admin specific endpoints
                        .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")

                        // Landlord specific endpoints
                        .requestMatchers("/api/landlord/**").hasAnyAuthority("ROLE_LANDLORD", "ROLE_ADMIN")

                        // Renter specific endpoints
                        .requestMatchers("/api/renter/**").hasAnyAuthority("ROLE_RENTER", "ROLE_ADMIN")

                        // General authenticated user endpoints
                        .requestMatchers(
                            "/api/v1/users/**", 
                            "/api/conversations/**",
                            "/api/messages/**",
                            "/api/reviews/**",
                            "/api/bookings"
                        ).authenticated()

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
            "http://localhost:3000",
            "http://localhost:3001",
            "http://localhost:5173",
            "https://terrarent.vercel.app"
        )); // Allow frontend origins
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
