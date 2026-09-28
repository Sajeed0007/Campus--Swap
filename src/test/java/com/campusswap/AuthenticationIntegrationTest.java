package com.campusswap;

import com.campusswap.dto.request.LoginRequest;
import com.campusswap.dto.request.RegisterRequest;
import com.campusswap.dto.response.AuthResponse;
import com.campusswap.model.User;
import com.campusswap.model.enums.Role;
import com.campusswap.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthenticationIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }
    
    @Test
    void testRegisterNewUser() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("John Doe")
                .email("john@college.edu")
                .password("password123")
                .college("Test College")
                .phoneNumber("1234567890")
                .build();
        
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("john@college.edu"))
                .andExpect(jsonPath("$.fullName").value("John Doe"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.token").exists())
                .andReturn();
        
        String responseBody = result.getResponse().getContentAsString();
        AuthResponse authResponse = objectMapper.readValue(responseBody, AuthResponse.class);
        
        assertThat(authResponse.getToken()).isNotNull();
        assertThat(authResponse.getTokenType()).isEqualTo("Bearer");
    }
    
    @Test
    void testRegisterWithExistingEmail() throws Exception {
        // Create existing user
        User existingUser = User.builder()
                .email("existing@college.edu")
                .password(passwordEncoder.encode("password123"))
                .fullName("Existing User")
                .college("Test College")
                .role(Role.STUDENT)
                .build();
        userRepository.save(existingUser);
        
        RegisterRequest request = RegisterRequest.builder()
                .fullName("New User")
                .email("existing@college.edu")
                .password("password123")
                .college("Test College")
                .build();
        
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email already exists: existing@college.edu"));
    }
    
    @Test
    void testLoginWithValidCredentials() throws Exception {
        // Create user
        User user = User.builder()
                .email("user@college.edu")
                .password(passwordEncoder.encode("password123"))
                .fullName("Test User")
                .college("Test College")
                .role(Role.STUDENT)
                .build();
        userRepository.save(user);
        
        LoginRequest request = LoginRequest.builder()
                .email("user@college.edu")
                .password("password123")
                .build();
        
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@college.edu"))
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }
    
    @Test
    void testLoginWithInvalidPassword() throws Exception {
        // Create user
        User user = User.builder()
                .email("user@college.edu")
                .password(passwordEncoder.encode("password123"))
                .fullName("Test User")
                .college("Test College")
                .role(Role.STUDENT)
                .build();
        userRepository.save(user);
        
        LoginRequest request = LoginRequest.builder()
                .email("user@college.edu")
                .password("wrongpassword")
                .build();
        
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }
    
    @Test
    void testLoginWithNonExistentUser() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("nonexistent@college.edu")
                .password("password123")
                .build();
        
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void testAccessProtectedEndpointWithoutToken() throws Exception {
        // No credentials at all must be 401 (not 403), so the client knows to log in.
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void testAccessProtectedEndpointWithValidToken() throws Exception {
        // Register user and get token
        RegisterRequest registerRequest = RegisterRequest.builder()
                .fullName("Test User")
                .email("test@college.edu")
                .password("password123")
                .college("Test College")
                .build();
        
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        
        String registerResponse = registerResult.getResponse().getContentAsString();
        AuthResponse authResponse = objectMapper.readValue(registerResponse, AuthResponse.class);
        String token = authResponse.getToken();
        
        // Access protected endpoint
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@college.edu"));
    }
    
    @Test
    void testPublicEndpointAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/listings"))
                .andExpect(status().isOk());
    }
}
