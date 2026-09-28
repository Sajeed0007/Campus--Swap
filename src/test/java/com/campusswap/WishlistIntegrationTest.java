package com.campusswap;

import com.campusswap.dto.request.RegisterRequest;
import com.campusswap.dto.response.AuthResponse;
import com.campusswap.model.Listing;
import com.campusswap.model.User;
import com.campusswap.model.Wishlist;
import com.campusswap.model.enums.*;
import com.campusswap.repository.ListingRepository;
import com.campusswap.repository.UserRepository;
import com.campusswap.repository.WishlistRepository;
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

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WishlistIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ListingRepository listingRepository;
    
    @Autowired
    private WishlistRepository wishlistRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    private String studentToken;
    private User student;
    private User currentUser;
    private Listing listing;
    
    @BeforeEach
    void setUp() throws Exception {
        wishlistRepository.deleteAll();
        listingRepository.deleteAll();
        userRepository.deleteAll();
        
        // Create student user
        student = User.builder()
                .email("seller@college.edu")
                .password(passwordEncoder.encode("password123"))
                .fullName("Seller")
                .college("Test College")
                .role(Role.STUDENT)
                .build();
        student = userRepository.save(student);
        
        // Create listing
        listing = Listing.builder()
                .title("Test Listing")
                .description("Description")
                .price(new BigDecimal("20.00"))
                .category(Category.BOOKS)
                .transactionType(TransactionType.SELL)
                .itemCondition(ItemCondition.GOOD)
                .status(ListingStatus.AVAILABLE)
                .seller(student)
                .build();
        listing = listingRepository.save(listing);
        
        // Register new student and get token
        RegisterRequest registerRequest = RegisterRequest.builder()
                .fullName("Buyer Student")
                .email("buyer@college.edu")
                .password("password123")
                .college("Test College")
                .build();
        
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        
        AuthResponse authResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);
        studentToken = authResponse.getToken();
        currentUser = userRepository.findByEmail("buyer@college.edu").get();
    }
    
    @Test
    void testAddToWishlist() throws Exception {
        mockMvc.perform(post("/api/wishlist/" + listing.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.listing.id").value(listing.getId()))
                .andExpect(jsonPath("$.listing.title").value("Test Listing"))
                .andExpect(jsonPath("$.addedAt").exists());
    }
    
    @Test
    void testAddToWishlistWithoutAuth() throws Exception {
        mockMvc.perform(post("/api/wishlist/" + listing.getId()))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void testAddDuplicateToWishlist() throws Exception {
        // Add once
        mockMvc.perform(post("/api/wishlist/" + listing.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isCreated());
        
        // Try to add again
        mockMvc.perform(post("/api/wishlist/" + listing.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Listing already in wishlist"));
    }
    
    @Test
    void testGetWishlist() throws Exception {
        // Add items to wishlist
        Wishlist wishlist1 = Wishlist.builder()
                .user(currentUser)
                .listing(listing)
                .build();
        wishlistRepository.save(wishlist1);
        
        Listing listing2 = Listing.builder()
                .title("Another Listing")
                .description("Description")
                .price(new BigDecimal("30.00"))
                .category(Category.ELECTRONICS)
                .transactionType(TransactionType.SELL)
                .itemCondition(ItemCondition.LIKE_NEW)
                .status(ListingStatus.AVAILABLE)
                .seller(student)
                .build();
        listing2 = listingRepository.save(listing2);
        
        Wishlist wishlist2 = Wishlist.builder()
                .user(currentUser)
                .listing(listing2)
                .build();
        wishlistRepository.save(wishlist2);
        
        mockMvc.perform(get("/api/wishlist")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].listing").exists())
                .andExpect(jsonPath("$[1].listing").exists());
    }
    
    @Test
    void testRemoveFromWishlist() throws Exception {
        // Add to wishlist
        Wishlist wishlist = Wishlist.builder()
                .user(currentUser)
                .listing(listing)
                .build();
        wishlistRepository.save(wishlist);
        
        // Remove from wishlist
        mockMvc.perform(delete("/api/wishlist/" + listing.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isNoContent());
        
        // Verify removed
        mockMvc.perform(get("/api/wishlist")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
    
    @Test
    void testCheckIfInWishlist() throws Exception {
        // Not in wishlist
        mockMvc.perform(get("/api/wishlist/check/" + listing.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
        
        // Add to wishlist
        Wishlist wishlist = Wishlist.builder()
                .user(currentUser)
                .listing(listing)
                .build();
        wishlistRepository.save(wishlist);
        
        // Check again
        mockMvc.perform(get("/api/wishlist/check/" + listing.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }
}
