package com.campusswap;

import com.campusswap.dto.request.CreateListingRequest;
import com.campusswap.dto.request.RegisterRequest;
import com.campusswap.dto.response.AuthResponse;
import com.campusswap.model.Listing;
import com.campusswap.model.User;
import com.campusswap.model.enums.*;
import com.campusswap.repository.ListingRepository;
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

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ListingIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ListingRepository listingRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    private String studentToken;
    private User student;
    private User admin;
    
    @BeforeEach
    void setUp() throws Exception {
        listingRepository.deleteAll();
        userRepository.deleteAll();
        
        // Create student user
        student = User.builder()
                .email("student@college.edu")
                .password(passwordEncoder.encode("password123"))
                .fullName("Test Student")
                .college("Test College")
                .role(Role.STUDENT)
                .build();
        student = userRepository.save(student);
        
        // Create admin user
        admin = User.builder()
                .email("admin@college.edu")
                .password(passwordEncoder.encode("adminpass"))
                .fullName("Test Admin")
                .college("Test College")
                .role(Role.ADMIN)
                .build();
        admin = userRepository.save(admin);
        
        // Get student token
        RegisterRequest registerRequest = RegisterRequest.builder()
                .fullName("New Student")
                .email("newstudent@college.edu")
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
    }
    
    @Test
    void testCreateListing() throws Exception {
        CreateListingRequest request = CreateListingRequest.builder()
                .title("Data Structures Textbook")
                .description("Excellent condition, like new")
                .price(new BigDecimal("45.00"))
                .category(Category.BOOKS)
                .transactionType(TransactionType.SELL)
                .itemCondition(ItemCondition.LIKE_NEW)
                .build();
        
        mockMvc.perform(post("/api/listings")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Data Structures Textbook"))
                .andExpect(jsonPath("$.description").value("Excellent condition, like new"))
                .andExpect(jsonPath("$.price").value(45.00))
                .andExpect(jsonPath("$.category").value("BOOKS"))
                .andExpect(jsonPath("$.transactionType").value("SELL"))
                .andExpect(jsonPath("$.itemCondition").value("LIKE_NEW"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.seller").exists())
                // SellerInfoResponse intentionally omits email; it exposes name/phone/college only.
                .andExpect(jsonPath("$.seller.fullName").value("New Student"))
                .andExpect(jsonPath("$.seller.email").doesNotExist());
    }
    
    @Test
    void testCreateListingWithoutAuthentication() throws Exception {
        CreateListingRequest request = CreateListingRequest.builder()
                .title("Test Item")
                .description("Test description")
                .price(new BigDecimal("10.00"))
                .category(Category.BOOKS)
                .transactionType(TransactionType.SELL)
                .itemCondition(ItemCondition.GOOD)
                .build();
        
        mockMvc.perform(post("/api/listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void testGetListingById() throws Exception {
        // Create listing
        Listing listing = Listing.builder()
                .title("Test Listing")
                .description("Test description")
                .price(new BigDecimal("25.00"))
                .category(Category.ELECTRONICS)
                .transactionType(TransactionType.SELL)
                .itemCondition(ItemCondition.GOOD)
                .status(ListingStatus.AVAILABLE)
                .seller(student)
                .build();
        listing = listingRepository.save(listing);
        
        mockMvc.perform(get("/api/listings/" + listing.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Listing"))
                .andExpect(jsonPath("$.price").value(25.00))
                .andExpect(jsonPath("$.category").value("ELECTRONICS"));
    }
    
    @Test
    void testGetAllListings() throws Exception {
        // Create multiple listings
        for (int i = 1; i <= 5; i++) {
            Listing listing = Listing.builder()
                    .title("Listing " + i)
                    .description("Description " + i)
                    .price(new BigDecimal(i * 10))
                    .category(Category.BOOKS)
                    .transactionType(TransactionType.SELL)
                    .itemCondition(ItemCondition.GOOD)
                    .status(ListingStatus.AVAILABLE)
                    .seller(student)
                    .build();
            listingRepository.save(listing);
        }
        
        mockMvc.perform(get("/api/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.page").value(0));
    }
    
    @Test
    void testGetListingsWithPagination() throws Exception {
        // Create 25 listings
        for (int i = 1; i <= 25; i++) {
            Listing listing = Listing.builder()
                    .title("Listing " + i)
                    .description("Description " + i)
                    .price(new BigDecimal(i * 10))
                    .category(Category.BOOKS)
                    .transactionType(TransactionType.SELL)
                    .itemCondition(ItemCondition.GOOD)
                    .status(ListingStatus.AVAILABLE)
                    .seller(student)
                    .build();
            listingRepository.save(listing);
        }
        
        // Get first page
        mockMvc.perform(get("/api/listings?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(10)))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));
        
        // Get second page
        mockMvc.perform(get("/api/listings?page=1&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(10)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(false));
    }
    
    @Test
    void testSearchListingsByTitle() throws Exception {
        Listing listing1 = Listing.builder()
                .title("Java Programming Book")
                .description("Learn Java")
                .price(new BigDecimal("30.00"))
                .category(Category.BOOKS)
                .transactionType(TransactionType.SELL)
                .itemCondition(ItemCondition.GOOD)
                .status(ListingStatus.AVAILABLE)
                .seller(student)
                .build();
        listingRepository.save(listing1);
        
        Listing listing2 = Listing.builder()
                .title("Python Programming Book")
                .description("Learn Python")
                .price(new BigDecimal("25.00"))
                .category(Category.BOOKS)
                .transactionType(TransactionType.SELL)
                .itemCondition(ItemCondition.GOOD)
                .status(ListingStatus.AVAILABLE)
                .seller(student)
                .build();
        listingRepository.save(listing2);
        
        mockMvc.perform(get("/api/listings?search=Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Java Programming Book"));
    }
    
    @Test
    void testFilterListingsByCategory() throws Exception {
        Listing bookListing = Listing.builder()
                .title("Book")
                .description("A book")
                .price(new BigDecimal("20.00"))
                .category(Category.BOOKS)
                .transactionType(TransactionType.SELL)
                .itemCondition(ItemCondition.GOOD)
                .status(ListingStatus.AVAILABLE)
                .seller(student)
                .build();
        listingRepository.save(bookListing);
        
        Listing electronicsListing = Listing.builder()
                .title("Laptop")
                .description("A laptop")
                .price(new BigDecimal("500.00"))
                .category(Category.ELECTRONICS)
                .transactionType(TransactionType.SELL)
                .itemCondition(ItemCondition.GOOD)
                .status(ListingStatus.AVAILABLE)
                .seller(student)
                .build();
        listingRepository.save(electronicsListing);
        
        mockMvc.perform(get("/api/listings?category=BOOKS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].category").value("BOOKS"));
    }
    
    @Test
    void testFilterListingsByTransactionType() throws Exception {
        Listing sellListing = Listing.builder()
                .title("For Sale")
                .description("Selling")
                .price(new BigDecimal("20.00"))
                .category(Category.BOOKS)
                .transactionType(TransactionType.SELL)
                .itemCondition(ItemCondition.GOOD)
                .status(ListingStatus.AVAILABLE)
                .seller(student)
                .build();
        listingRepository.save(sellListing);
        
        Listing donateListing = Listing.builder()
                .title("Free")
                .description("Donating")
                .price(null)
                .category(Category.BOOKS)
                .transactionType(TransactionType.DONATE)
                .itemCondition(ItemCondition.FAIR)
                .status(ListingStatus.AVAILABLE)
                .seller(student)
                .build();
        listingRepository.save(donateListing);
        
        mockMvc.perform(get("/api/listings?transactionType=DONATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].transactionType").value("DONATE"));
    }
    
    @Test
    void testFilterListingsByPriceRange() throws Exception {
        for (int i = 1; i <= 5; i++) {
            Listing listing = Listing.builder()
                    .title("Item " + i)
                    .description("Description")
                    .price(new BigDecimal(i * 10))
                    .category(Category.BOOKS)
                    .transactionType(TransactionType.SELL)
                    .itemCondition(ItemCondition.GOOD)
                    .status(ListingStatus.AVAILABLE)
                    .seller(student)
                    .build();
            listingRepository.save(listing);
        }
        
        mockMvc.perform(get("/api/listings?minPrice=20&maxPrice=40"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)));
    }
    
    @Test
    void testUpdateListing() throws Exception {
        Listing listing = Listing.builder()
                .title("Original Title")
                .description("Original description")
                .price(new BigDecimal("20.00"))
                .category(Category.BOOKS)
                .transactionType(TransactionType.SELL)
                .itemCondition(ItemCondition.GOOD)
                .status(ListingStatus.AVAILABLE)
                .seller(userRepository.findByEmail("newstudent@college.edu").get())
                .build();
        listing = listingRepository.save(listing);
        
        String updateJson = """
                {
                    "title": "Updated Title",
                    "description": "Updated description",
                    "price": 25.00
                }
                """;
        
        mockMvc.perform(put("/api/listings/" + listing.getId())
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.price").value(25.00));
    }
    
    @Test
    void testUpdateListingUnauthorized() throws Exception {
        Listing listing = Listing.builder()
                .title("Someone else's listing")
                .description("Description")
                .price(new BigDecimal("20.00"))
                .category(Category.BOOKS)
                .transactionType(TransactionType.SELL)
                .itemCondition(ItemCondition.GOOD)
                .status(ListingStatus.AVAILABLE)
                .seller(student)
                .build();
        listing = listingRepository.save(listing);
        
        String updateJson = """
                {
                    "title": "Hacked Title"
                }
                """;
        
        mockMvc.perform(put("/api/listings/" + listing.getId())
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isForbidden());
    }
    
    @Test
    void testDeleteListing() throws Exception {
        Listing listing = Listing.builder()
                .title("To Delete")
                .description("Description")
                .price(new BigDecimal("20.00"))
                .category(Category.BOOKS)
                .transactionType(TransactionType.SELL)
                .itemCondition(ItemCondition.GOOD)
                .status(ListingStatus.AVAILABLE)
                .seller(userRepository.findByEmail("newstudent@college.edu").get())
                .build();
        listing = listingRepository.save(listing);
        
        mockMvc.perform(delete("/api/listings/" + listing.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isNoContent());
        
        // Verify listing is deleted
        mockMvc.perform(get("/api/listings/" + listing.getId()))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void testMarkListingAsSold() throws Exception {
        Listing listing = Listing.builder()
                .title("To Mark Sold")
                .description("Description")
                .price(new BigDecimal("20.00"))
                .category(Category.BOOKS)
                .transactionType(TransactionType.SELL)
                .itemCondition(ItemCondition.GOOD)
                .status(ListingStatus.AVAILABLE)
                .seller(userRepository.findByEmail("newstudent@college.edu").get())
                .build();
        listing = listingRepository.save(listing);
        
        mockMvc.perform(patch("/api/listings/" + listing.getId() + "/mark-sold")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SOLD"));
    }
    
    @Test
    void testGetMyListings() throws Exception {
        User currentUser = userRepository.findByEmail("newstudent@college.edu").get();
        
        for (int i = 1; i <= 3; i++) {
            Listing listing = Listing.builder()
                    .title("My Listing " + i)
                    .description("Description")
                    .price(new BigDecimal(i * 10))
                    .category(Category.BOOKS)
                    .transactionType(TransactionType.SELL)
                    .itemCondition(ItemCondition.GOOD)
                    .status(ListingStatus.AVAILABLE)
                    .seller(currentUser)
                    .build();
            listingRepository.save(listing);
        }
        
        // Create listing for another user
        Listing otherListing = Listing.builder()
                .title("Other User's Listing")
                .description("Description")
                .price(new BigDecimal("50.00"))
                .category(Category.BOOKS)
                .transactionType(TransactionType.SELL)
                .itemCondition(ItemCondition.GOOD)
                .status(ListingStatus.AVAILABLE)
                .seller(student)
                .build();
        listingRepository.save(otherListing);
        
        mockMvc.perform(get("/api/listings/my-listings")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].title", everyItem(containsString("My Listing"))));
    }
    
    @Test
    void testOnlyAvailableListingsShownInPublicBrowse() throws Exception {
        // Create AVAILABLE listing
        Listing availableListing = Listing.builder()
                .title("Available")
                .description("Description")
                .price(new BigDecimal("20.00"))
                .category(Category.BOOKS)
                .transactionType(TransactionType.SELL)
                .itemCondition(ItemCondition.GOOD)
                .status(ListingStatus.AVAILABLE)
                .seller(student)
                .build();
        listingRepository.save(availableListing);
        
        // Create SOLD listing
        Listing soldListing = Listing.builder()
                .title("Sold")
                .description("Description")
                .price(new BigDecimal("20.00"))
                .category(Category.BOOKS)
                .transactionType(TransactionType.SELL)
                .itemCondition(ItemCondition.GOOD)
                .status(ListingStatus.SOLD)
                .seller(student)
                .build();
        listingRepository.save(soldListing);
        
        // Create REMOVED listing
        Listing removedListing = Listing.builder()
                .title("Removed")
                .description("Description")
                .price(new BigDecimal("20.00"))
                .category(Category.BOOKS)
                .transactionType(TransactionType.SELL)
                .itemCondition(ItemCondition.GOOD)
                .status(ListingStatus.REMOVED)
                .seller(student)
                .build();
        listingRepository.save(removedListing);
        
        mockMvc.perform(get("/api/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Available"));
    }
}
