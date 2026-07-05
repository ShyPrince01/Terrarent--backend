package com.terrarent.config;

import com.terrarent.security.JwtService;
import com.terrarent.repository.UserRepository;
import com.terrarent.repository.RoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.terrarent.entity.Role;
import com.terrarent.entity.User;
import io.jsonwebtoken.Claims;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final @Lazy UserDetailsService userDetailsService; 
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        String path = request.getServletPath();
        if (path.startsWith("/api/auth") || path.startsWith("/h2-console")) {
            filterChain.doFilter(request, response);
            return;
        }
        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        final String jwt = authHeader.substring(7);
        String userEmail;
        try {
            userEmail = jwtService.extractUsername(jwt);
        } catch (Exception e) {
            filterChain.doFilter(request, response);
            return;
        }
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = null;
            try {
                userDetails = userDetailsService.loadUserByUsername(userEmail);
            } catch (UsernameNotFoundException ex) {
                // If allowed, create user from token claims
                if (jwtService.isAllowInsecureParse()) {
                    try {
                        if (!userRepository.existsByEmail(userEmail)) {
                            Claims claims = jwtService.extractAllClaimsPublic(jwt);
                            String rolesClaim = null;
                            Object rolesObj = claims.get("roles");
                            if (rolesObj != null) rolesClaim = rolesObj.toString();
                            Role.RoleName roleName = Role.RoleName.ROLE_RENTER;
                            if (rolesClaim != null) {
                                try {
                                    roleName = Role.RoleName.fromString(rolesClaim);
                                } catch (Exception ignore) {
                                }
                            }
                            final Role.RoleName resolvedRoleName = roleName;
                            Role role = roleRepository.findByName(resolvedRoleName)
                                    .orElseThrow(() -> new RuntimeException("Role not found: " + resolvedRoleName));
                            String localPart = userEmail.split("@")[0];
                            User newUser = User.builder()
                                    .firstName(localPart)
                                    .lastName("Imported")
                                    .email(userEmail)
                                    .password(passwordEncoder.encode(java.util.UUID.randomUUID().toString()))
                                    .phoneNumber(null)
                                    .status(User.UserStatus.VERIFIED)
                                    .role(role)
                                    .build();
                            userRepository.save(newUser);
                        }
                        userDetails = userDetailsService.loadUserByUsername(userEmail);
                    } catch (Exception createEx) {
                        // fall through without authentication
                    }
                }
            }
            if (userDetails != null && jwtService.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        filterChain.doFilter(request, response);
    }
}