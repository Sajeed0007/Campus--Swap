package com.campusswap.service;

import com.campusswap.dto.request.LoginRequest;
import com.campusswap.dto.request.RegisterRequest;
import com.campusswap.dto.response.AuthResponse;
import com.campusswap.exception.BadRequestException;
import com.campusswap.model.User;
import com.campusswap.model.enums.Role;
import com.campusswap.repository.UserRepository;
import com.campusswap.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());
        
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists: " + request.getEmail());
        }
        
        // Create new user
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .college(request.getCollege())
                .phoneNumber(request.getPhoneNumber())
                .hostelOrDorm(request.getHostelOrDorm())
                .role(Role.STUDENT) // Default role
                .build();
        
        User savedUser = userRepository.save(user);
        
        // Generate JWT token
        String token = jwtUtil.generateToken(savedUser.getEmail(), savedUser.getRole().name());
        
        log.info("User registered successfully with ID: {}", savedUser.getId());
        
        return AuthResponse.builder()
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .college(savedUser.getCollege())
                .role(savedUser.getRole())
                .token(token)
                .tokenType("Bearer")
                .build();
    }
    
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("User login attempt with email: {}", request.getEmail());
        
        try {
            // Throws on bad credentials; the returned Authentication is not needed
            // because authorities are re-resolved per request by the JWT filter.
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
            
            // Get user details
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
            
            // Generate JWT token
            String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
            
            log.info("User logged in successfully: {}", user.getEmail());
            
            return AuthResponse.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .college(user.getCollege())
                    .role(user.getRole())
                    .token(token)
                    .tokenType("Bearer")
                    .build();
            
        } catch (AuthenticationException e) {
            // A wrong password is routine, not an application error: keep it at WARN
            // so real faults stay visible in the error log.
            log.warn("Authentication failed for email: {}", request.getEmail());
            throw new BadCredentialsException("Invalid email or password");
        }
    }
}
