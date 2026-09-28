package com.campusswap;

import com.campusswap.dto.request.CreateListingRequest;
import com.campusswap.dto.request.CreateReportRequest;
import com.campusswap.dto.request.LoginRequest;
import com.campusswap.dto.request.RegisterRequest;
import com.campusswap.dto.response.AuthResponse;
import com.campusswap.model.Listing;
import com.campusswap.model.User;
import com.campusswap.model.enums.Category;
import com.campusswap.model.enums.ItemCondition;
import com.campusswap.model.enums.ListingStatus;
import com.campusswap.model.enums.Role;
import com.campusswap.model.enums.TransactionType;
import com.campusswap.repository.ListingRepository;
import com.campusswap.repository.ReportRepository;
import com.campusswap.repository.UserRepository;
import com.campusswap.repository.WishlistRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the production-readiness scenarios end to end through the HTTP layer:
 * duplicate registration, invalid login, invalid token, unauthorized edit/delete,
 * missing listing, invalid price, empty search, wishlist add/remove, reporting,
 * and marking a listing sold.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProductionReadinessIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private ListingRepository listingRepository;
    @Autowired private WishlistRepository wishlistRepository;
    @Autowired private ReportRepository reportRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private static final String OWNER_EMAIL = "owner@college.edu";
    private static final String OTHER_EMAIL = "other@college.edu";
    private static final String PASSWORD = "password123";

    private String ownerToken;
    private String otherToken;
    private String adminToken;
    private User owner;
    private Listing ownerListing;

    @BeforeEach
    void setUp() throws Exception {
        reportRepository.deleteAll();
        wishlistRepository.deleteAll();
        listingRepository.deleteAll();
        userRepository.deleteAll();

        ownerToken = registerAndLogin("Listing Owner", OWNER_EMAIL);
        otherToken = registerAndLogin("Other Student", OTHER_EMAIL);
        owner = userRepository.findByEmail(OWNER_EMAIL).orElseThrow();

        User admin = userRepository.save(User.builder()
                .email("admin@college.edu")
                .password(passwordEncoder.encode(PASSWORD))
                .fullName("Admin User")
                .college("Test College")
                .role(Role.ADMIN)
                .build());
        adminToken = login(admin.getEmail(), PASSWORD);

        ownerListing = listingRepository.save(Listing.builder()
                .title("Algorithms Textbook")
                .description("Third edition, light wear")
                .price(new BigDecimal("40.00"))
                .category(Category.BOOKS)
                .transactionType(TransactionType.SELL)
                .itemCondition(ItemCondition.GOOD)
                .status(ListingStatus.AVAILABLE)
                .seller(owner)
                .build());
    }

    // ---------- helpers ----------

    private String registerAndLogin(String fullName, String email) throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .fullName(fullName)
                .email(email)
                .password(PASSWORD)
                .college("Test College")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper
                .readValue(result.getResponse().getContentAsString(), AuthResponse.class)
                .getToken();
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                LoginRequest.builder().email(email).password(password).build())))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper
                .readValue(result.getResponse().getContentAsString(), AuthResponse.class)
                .getToken();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    // ---------- 1. duplicate registration ----------

    @Nested
    @DisplayName("Duplicate registration")
    class DuplicateRegistration {

        @Test
        @DisplayName("rejects an email that already exists")
        void rejectsDuplicateEmail() throws Exception {
            RegisterRequest duplicate = RegisterRequest.builder()
                    .fullName("Impostor")
                    .email(OWNER_EMAIL)
                    .password(PASSWORD)
                    .college("Test College")
                    .build();

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(duplicate)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value("Email already exists: " + OWNER_EMAIL));
        }

        @Test
        @DisplayName("does not create a second user record")
        void doesNotCreateSecondRecord() throws Exception {
            long before = userRepository.count();

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(RegisterRequest.builder()
                                    .fullName("Impostor")
                                    .email(OWNER_EMAIL)
                                    .password(PASSWORD)
                                    .college("Test College")
                                    .build())))
                    .andExpect(status().isBadRequest());

            org.junit.jupiter.api.Assertions.assertEquals(before, userRepository.count());
        }

        @Test
        @DisplayName("rejects a registration missing required fields")
        void rejectsMissingFields() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"no-name@college.edu\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.validationErrors.fullName").exists())
                    .andExpect(jsonPath("$.validationErrors.password").exists())
                    .andExpect(jsonPath("$.validationErrors.college").exists());
        }

        @Test
        @DisplayName("never echoes the password back")
        void neverReturnsPassword() throws Exception {
            MvcResult result = mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(RegisterRequest.builder()
                                    .fullName("Fresh User")
                                    .email("fresh@college.edu")
                                    .password("supersecret")
                                    .college("Test College")
                                    .build())))
                    .andExpect(status().isCreated())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            org.junit.jupiter.api.Assertions.assertFalse(
                    responseBody.contains("supersecret"),
                    "Registration response must not contain the raw password");
            org.junit.jupiter.api.Assertions.assertFalse(
                    responseBody.contains("\"password\""),
                    "Registration response must not contain a password field");

            // The stored password must be a BCrypt hash, never the plaintext.
            String stored = userRepository.findByEmail("fresh@college.edu").orElseThrow().getPassword();
            org.junit.jupiter.api.Assertions.assertNotEquals("supersecret", stored);
            org.junit.jupiter.api.Assertions.assertTrue(
                    stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$"),
                    "Password must be BCrypt hashed, was: " + stored.substring(0, Math.min(4, stored.length())));
        }
    }

    // ---------- 2. invalid login ----------

    @Nested
    @DisplayName("Invalid login")
    class InvalidLogin {

        @Test
        @DisplayName("wrong password returns 401")
        void wrongPassword() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(LoginRequest.builder()
                                    .email(OWNER_EMAIL)
                                    .password("wrong-password")
                                    .build())))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid email or password"));
        }

        @Test
        @DisplayName("unknown email returns the same generic 401 (no user enumeration)")
        void unknownEmailIsIndistinguishable() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(LoginRequest.builder()
                                    .email("nobody@college.edu")
                                    .password(PASSWORD)
                                    .build())))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid email or password"));
        }

        @Test
        @DisplayName("malformed body returns 400, not 500")
        void malformedBody() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{not valid json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Malformed request body."));
        }
    }

    // ---------- 3. invalid token ----------

    @Nested
    @DisplayName("Invalid token")
    class InvalidToken {

        @Test
        @DisplayName("garbage bearer token is treated as unauthenticated")
        void garbageToken() throws Exception {
            mockMvc.perform(get("/api/users/me")
                            .header("Authorization", "Bearer not-a-real-jwt"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("token signed with a different key is rejected")
        void wrongSignature() throws Exception {
            // Structurally valid JWT, signed with an unrelated key.
            String forged = "eyJhbGciOiJIUzI1NiJ9"
                    + ".eyJzdWIiOiJvd25lckBjb2xsZWdlLmVkdSIsImV4cCI6OTk5OTk5OTk5OX0"
                    + ".QWxsWW91ckJhc2VBcmVCZWxvbmdUb1VzMDAwMDAwMDAwMDAwMDA";

            mockMvc.perform(get("/api/users/me")
                            .header("Authorization", bearer(forged)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("missing Bearer prefix is ignored")
        void missingBearerPrefix() throws Exception {
            mockMvc.perform(get("/api/users/me")
                            .header("Authorization", ownerToken))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("a valid token still works")
        void validTokenSucceeds() throws Exception {
            mockMvc.perform(get("/api/users/me")
                            .header("Authorization", bearer(ownerToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(OWNER_EMAIL));
        }
    }

    // ---------- 4 & 5. ownership enforcement ----------

    @Nested
    @DisplayName("Listing ownership")
    class Ownership {

        @Test
        @DisplayName("another student cannot edit the listing")
        void cannotEditOthersListing() throws Exception {
            mockMvc.perform(put("/api/listings/" + ownerListing.getId())
                            .header("Authorization", bearer(otherToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Hijacked\"}"))
                    .andExpect(status().isForbidden());

            // Confirm nothing was mutated.
            mockMvc.perform(get("/api/listings/" + ownerListing.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Algorithms Textbook"));
        }

        @Test
        @DisplayName("another student cannot delete the listing")
        void cannotDeleteOthersListing() throws Exception {
            mockMvc.perform(delete("/api/listings/" + ownerListing.getId())
                            .header("Authorization", bearer(otherToken)))
                    .andExpect(status().isForbidden());

            org.junit.jupiter.api.Assertions.assertTrue(
                    listingRepository.findById(ownerListing.getId()).isPresent(),
                    "Listing must still exist after a forbidden delete");
        }

        @Test
        @DisplayName("another student cannot mark the listing sold")
        void cannotMarkOthersListingSold() throws Exception {
            mockMvc.perform(patch("/api/listings/" + ownerListing.getId() + "/mark-sold")
                            .header("Authorization", bearer(otherToken)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("the owner can edit and delete")
        void ownerCanEditAndDelete() throws Exception {
            mockMvc.perform(put("/api/listings/" + ownerListing.getId())
                            .header("Authorization", bearer(ownerToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Algorithms Textbook (4th ed)\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Algorithms Textbook (4th ed)"));

            mockMvc.perform(delete("/api/listings/" + ownerListing.getId())
                            .header("Authorization", bearer(ownerToken)))
                    .andExpect(status().isNoContent());
        }
    }

    // ---------- 6. missing listing ----------

    @Nested
    @DisplayName("Missing listing")
    class MissingListing {

        private static final long ABSENT_ID = 9_999_999L;

        @Test
        @DisplayName("GET returns 404 with a structured body")
        void getReturns404() throws Exception {
            mockMvc.perform(get("/api/listings/" + ABSENT_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.path").value("/api/listings/" + ABSENT_ID));
        }

        @Test
        @DisplayName("PUT returns 404")
        void putReturns404() throws Exception {
            mockMvc.perform(put("/api/listings/" + ABSENT_ID)
                            .header("Authorization", bearer(ownerToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Nothing here\"}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("DELETE returns 404")
        void deleteReturns404() throws Exception {
            mockMvc.perform(delete("/api/listings/" + ABSENT_ID)
                            .header("Authorization", bearer(ownerToken)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("adding a missing listing to the wishlist returns 404")
        void wishlistReturns404() throws Exception {
            mockMvc.perform(post("/api/wishlist/" + ABSENT_ID)
                            .header("Authorization", bearer(ownerToken)))
                    .andExpect(status().isNotFound());
        }
    }

    // ---------- 7. invalid price ----------

    @Nested
    @DisplayName("Invalid price")
    class InvalidPrice {

        private CreateListingRequest.CreateListingRequestBuilder base() {
            return CreateListingRequest.builder()
                    .title("Desk Lamp")
                    .description("Works fine")
                    .category(Category.HOSTEL_ITEMS)
                    .transactionType(TransactionType.SELL)
                    .itemCondition(ItemCondition.GOOD);
        }

        @Test
        @DisplayName("negative price is rejected")
        void negativePrice() throws Exception {
            mockMvc.perform(post("/api/listings")
                            .header("Authorization", bearer(ownerToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    base().price(new BigDecimal("-10.00")).build())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.validationErrors.price").exists());
        }

        @Test
        @DisplayName("zero price is rejected because the bound is exclusive")
        void zeroPrice() throws Exception {
            mockMvc.perform(post("/api/listings")
                            .header("Authorization", bearer(ownerToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    base().price(BigDecimal.ZERO).build())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.price").exists());
        }

        @Test
        @DisplayName("price with too many decimal places is rejected")
        void tooManyDecimals() throws Exception {
            mockMvc.perform(post("/api/listings")
                            .header("Authorization", bearer(ownerToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    base().price(new BigDecimal("10.12345")).build())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.price").exists());
        }

        @Test
        @DisplayName("blank title is rejected")
        void blankTitle() throws Exception {
            mockMvc.perform(post("/api/listings")
                            .header("Authorization", bearer(ownerToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    base().title("   ").price(new BigDecimal("5.00")).build())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.title").exists());
        }

        @Test
        @DisplayName("a valid price is accepted")
        void validPriceAccepted() throws Exception {
            mockMvc.perform(post("/api/listings")
                            .header("Authorization", bearer(ownerToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    base().price(new BigDecimal("12.50")).build())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.price").value(12.50));
        }
    }

    // ---------- 8. empty search result ----------

    @Nested
    @DisplayName("Empty search result")
    class EmptySearch {

        @Test
        @DisplayName("returns 200 with an empty page, not 404")
        void emptyPage() throws Exception {
            mockMvc.perform(get("/api/listings").param("search", "zzz-no-such-item-zzz"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.totalPages").value(0))
                    .andExpect(jsonPath("$.first").value(true));
        }

        @Test
        @DisplayName("a price window matching nothing yields an empty page")
        void priceWindowMatchesNothing() throws Exception {
            mockMvc.perform(get("/api/listings")
                            .param("minPrice", "100000")
                            .param("maxPrice", "200000"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("an unparseable category returns 400 rather than 500")
        void invalidEnumParam() throws Exception {
            mockMvc.perform(get("/api/listings").param("category", "NOT_A_CATEGORY"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid value for parameter 'category'."));
        }

        @Test
        @DisplayName("browsing requires no authentication")
        void browsingIsPublic() throws Exception {
            mockMvc.perform(get("/api/listings"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    // ---------- 9. wishlist add / remove ----------

    @Nested
    @DisplayName("Wishlist")
    class WishlistFlow {

        @Test
        @DisplayName("add, verify, then remove")
        void addThenRemove() throws Exception {
            long listingId = ownerListing.getId();

            mockMvc.perform(get("/api/wishlist/check/" + listingId)
                            .header("Authorization", bearer(otherToken)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("false"));

            mockMvc.perform(post("/api/wishlist/" + listingId)
                            .header("Authorization", bearer(otherToken)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.listing.id").value(listingId));

            mockMvc.perform(get("/api/wishlist")
                            .header("Authorization", bearer(otherToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            mockMvc.perform(delete("/api/wishlist/" + listingId)
                            .header("Authorization", bearer(otherToken)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/wishlist")
                            .header("Authorization", bearer(otherToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("adding twice is rejected")
        void duplicateAddRejected() throws Exception {
            long listingId = ownerListing.getId();

            mockMvc.perform(post("/api/wishlist/" + listingId)
                            .header("Authorization", bearer(otherToken)))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/api/wishlist/" + listingId)
                            .header("Authorization", bearer(otherToken)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Listing already in wishlist"));
        }

        @Test
        @DisplayName("removing something not saved returns 404")
        void removeUnsavedReturns404() throws Exception {
            mockMvc.perform(delete("/api/wishlist/" + ownerListing.getId())
                            .header("Authorization", bearer(otherToken)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("wishlists are per user")
        void wishlistsAreIsolated() throws Exception {
            mockMvc.perform(post("/api/wishlist/" + ownerListing.getId())
                            .header("Authorization", bearer(otherToken)))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/api/wishlist")
                            .header("Authorization", bearer(ownerToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ---------- 10. report listing ----------

    @Nested
    @DisplayName("Report listing")
    class Reporting {

        private CreateReportRequest reportRequest() {
            return CreateReportRequest.builder()
                    .listingId(ownerListing.getId())
                    .reason("Listing looks like a scam")
                    .build();
        }

        @Test
        @DisplayName("a student can report a listing")
        void studentCanReport() throws Exception {
            mockMvc.perform(post("/api/reports")
                            .header("Authorization", bearer(otherToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reportRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.listingId").value(ownerListing.getId()));
        }

        @Test
        @DisplayName("reporting twice is rejected")
        void duplicateReportRejected() throws Exception {
            mockMvc.perform(post("/api/reports")
                            .header("Authorization", bearer(otherToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reportRequest())))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/api/reports")
                            .header("Authorization", bearer(otherToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reportRequest())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("You have already reported this listing"));
        }

        @Test
        @DisplayName("a blank reason is rejected")
        void blankReasonRejected() throws Exception {
            mockMvc.perform(post("/api/reports")
                            .header("Authorization", bearer(otherToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"listingId\":" + ownerListing.getId() + ",\"reason\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.reason").exists());
        }

        @Test
        @DisplayName("a student cannot read the moderation queue")
        void studentCannotListReports() throws Exception {
            mockMvc.perform(get("/api/reports")
                            .header("Authorization", bearer(otherToken)))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/api/reports/pending")
                            .header("Authorization", bearer(otherToken)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("an admin resolving a report takes the listing down")
        void adminResolveRemovesListing() throws Exception {
            MvcResult created = mockMvc.perform(post("/api/reports")
                            .header("Authorization", bearer(otherToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reportRequest())))
                    .andExpect(status().isCreated())
                    .andReturn();

            long reportId = objectMapper
                    .readTree(created.getResponse().getContentAsString())
                    .get("id").asLong();

            mockMvc.perform(patch("/api/reports/" + reportId + "/resolve")
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("RESOLVED"));

            // The listing is moderated out of the public browse feed.
            mockMvc.perform(get("/api/listings"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0));

            // And out of the seller's public profile listing.
            mockMvc.perform(get("/api/listings/user/" + owner.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("an admin dismissing a report leaves the listing available")
        void adminDismissKeepsListing() throws Exception {
            MvcResult created = mockMvc.perform(post("/api/reports")
                            .header("Authorization", bearer(otherToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reportRequest())))
                    .andExpect(status().isCreated())
                    .andReturn();

            long reportId = objectMapper
                    .readTree(created.getResponse().getContentAsString())
                    .get("id").asLong();

            mockMvc.perform(patch("/api/reports/" + reportId + "/dismiss")
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("DISMISSED"));

            mockMvc.perform(get("/api/listings/" + ownerListing.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("AVAILABLE"));
        }
    }

    // ---------- 11. mark listing sold ----------

    @Nested
    @DisplayName("Mark listing sold")
    class MarkSold {

        @Test
        @DisplayName("the owner can mark it sold")
        void ownerMarksSold() throws Exception {
            mockMvc.perform(patch("/api/listings/" + ownerListing.getId() + "/mark-sold")
                            .header("Authorization", bearer(ownerToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SOLD"));
        }

        @Test
        @DisplayName("a sold listing disappears from the public browse feed")
        void soldListingLeavesBrowseFeed() throws Exception {
            mockMvc.perform(get("/api/listings"))
                    .andExpect(jsonPath("$.totalElements").value(1));

            mockMvc.perform(patch("/api/listings/" + ownerListing.getId() + "/mark-sold")
                            .header("Authorization", bearer(ownerToken)))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/listings"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("a sold listing is still directly retrievable")
        void soldListingStillRetrievable() throws Exception {
            mockMvc.perform(patch("/api/listings/" + ownerListing.getId() + "/mark-sold")
                            .header("Authorization", bearer(ownerToken)))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/listings/" + ownerListing.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SOLD"));
        }

        @Test
        @DisplayName("marking sold requires authentication")
        void requiresAuth() throws Exception {
            mockMvc.perform(patch("/api/listings/" + ownerListing.getId() + "/mark-sold"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ---------- profile privacy and admin authorization ----------

    @Nested
    @DisplayName("Profile privacy")
    class ProfilePrivacy {

        @Test
        @DisplayName("a student cannot read another student's profile")
        void cannotReadOtherProfile() throws Exception {
            mockMvc.perform(get("/api/users/" + owner.getId())
                            .header("Authorization", bearer(otherToken)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("a student can read their own profile by id")
        void canReadOwnProfile() throws Exception {
            mockMvc.perform(get("/api/users/" + owner.getId())
                            .header("Authorization", bearer(ownerToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(OWNER_EMAIL));
        }

        @Test
        @DisplayName("an admin can read any profile")
        void adminCanReadAnyProfile() throws Exception {
            mockMvc.perform(get("/api/users/" + owner.getId())
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(OWNER_EMAIL));
        }

        @Test
        @DisplayName("no profile response ever contains a password field")
        void profileNeverLeaksPassword() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/users/me")
                            .header("Authorization", bearer(ownerToken)))
                    .andExpect(status().isOk())
                    .andReturn();

            org.junit.jupiter.api.Assertions.assertFalse(
                    result.getResponse().getContentAsString().contains("password"),
                    "Profile response must not expose the password");
        }
    }

    @Nested
    @DisplayName("Admin authorization")
    class AdminAuthorization {

        @Test
        @DisplayName("a student cannot delete a user")
        void studentCannotDeleteUser() throws Exception {
            mockMvc.perform(delete("/api/users/" + owner.getId())
                            .header("Authorization", bearer(otherToken)))
                    .andExpect(status().isForbidden());

            org.junit.jupiter.api.Assertions.assertTrue(
                    userRepository.findById(owner.getId()).isPresent());
        }

        @Test
        @DisplayName("registration cannot self-assign the ADMIN role")
        void registrationCannotEscalateRole() throws Exception {
            // "role" is not a field on RegisterRequest; any extra property must be
            // ignored and the account must still be created as a STUDENT.
            String payload = """
                    {
                      "fullName": "Would Be Admin",
                      "email": "escalate@college.edu",
                      "password": "password123",
                      "college": "Test College",
                      "role": "ADMIN"
                    }
                    """;

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.role").value("STUDENT"));

            org.junit.jupiter.api.Assertions.assertEquals(
                    Role.STUDENT,
                    userRepository.findByEmail("escalate@college.edu").orElseThrow().getRole());
        }

        @Test
        @DisplayName("an admin reaches the moderation queue")
        void adminReachesModerationQueue() throws Exception {
            mockMvc.perform(get("/api/reports/pending")
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isOk());
        }
    }
}
