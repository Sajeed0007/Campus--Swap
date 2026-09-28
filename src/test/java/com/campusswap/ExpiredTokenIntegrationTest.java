package com.campusswap;

import com.campusswap.dto.request.RegisterRequest;
import com.campusswap.dto.response.AuthResponse;
import com.campusswap.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies expired-token handling. A negative {@code jwt.expiration} makes every
 * issued token already expired, which exercises the real signing and parsing path
 * rather than a hand-crafted token.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "jwt.expiration=-1000")
@Transactional
class ExpiredTokenIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;

    private String expiredToken;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();

        RegisterRequest request = RegisterRequest.builder()
                .fullName("Expiring User")
                .email("expiring@college.edu")
                .password("password123")
                .college("Test College")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        expiredToken = objectMapper
                .readValue(result.getResponse().getContentAsString(), AuthResponse.class)
                .getToken();
    }

    @Test
    @DisplayName("an expired token is rejected with 401 on a protected endpoint")
    void expiredTokenRejected() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("an expired token cannot create a listing")
    void expiredTokenCannotCreateListing() throws Exception {
        String body = """
                {
                  "title": "Should not be created",
                  "description": "Attempt with an expired token",
                  "price": 10.00,
                  "category": "BOOKS",
                  "transactionType": "SELL",
                  "itemCondition": "GOOD"
                }
                """;

        mockMvc.perform(post("/api/listings")
                        .header("Authorization", "Bearer " + expiredToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("public browsing still works while holding an expired token")
    void publicBrowsingUnaffected() throws Exception {
        mockMvc.perform(get("/api/listings")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isOk());
    }
}
