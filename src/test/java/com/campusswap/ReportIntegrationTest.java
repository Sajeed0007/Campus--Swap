package com.campusswap;

import com.campusswap.dto.request.CreateReportRequest;
import com.campusswap.dto.request.RegisterRequest;
import com.campusswap.dto.response.AuthResponse;
import com.campusswap.model.Listing;
import com.campusswap.model.Report;
import com.campusswap.model.User;
import com.campusswap.model.enums.*;
import com.campusswap.repository.ListingRepository;
import com.campusswap.repository.ReportRepository;
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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReportIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ListingRepository listingRepository;
    
    @Autowired
    private ReportRepository reportRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    private String studentToken;
    private String adminToken;
    private User seller;
    private User reporter;
    private User admin;
    private Listing listing;
    
    @BeforeEach
    void setUp() throws Exception {
        reportRepository.deleteAll();
        listingRepository.deleteAll();
        userRepository.deleteAll();
        
        // Create seller
        seller = User.builder()
                .email("seller@college.edu")
                .password(passwordEncoder.encode("password123"))
                .fullName("Seller")
                .college("Test College")
                .role(Role.STUDENT)
                .build();
        seller = userRepository.save(seller);
        
        // Create admin
        admin = User.builder()
                .email("admin@college.edu")
                .password(passwordEncoder.encode("adminpass"))
                .fullName("Admin User")
                .college("Test College")
                .role(Role.ADMIN)
                .build();
        admin = userRepository.save(admin);
        
        // Create listing
        listing = Listing.builder()
                .title("Suspicious Listing")
                .description("Description")
                .price(new BigDecimal("20.00"))
                .category(Category.BOOKS)
                .transactionType(TransactionType.SELL)
                .itemCondition(ItemCondition.GOOD)
                .status(ListingStatus.AVAILABLE)
                .seller(seller)
                .build();
        listing = listingRepository.save(listing);
        
        // Register student reporter and get token
        RegisterRequest studentRequest = RegisterRequest.builder()
                .fullName("Reporter Student")
                .email("reporter@college.edu")
                .password("password123")
                .college("Test College")
                .build();
        
        MvcResult studentResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(studentRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        
        AuthResponse studentAuth = objectMapper.readValue(
                studentResult.getResponse().getContentAsString(), AuthResponse.class);
        studentToken = studentAuth.getToken();
        reporter = userRepository.findByEmail("reporter@college.edu").get();
        
        // Register admin and get token
        RegisterRequest adminRequest = RegisterRequest.builder()
                .fullName("Admin")
                .email("newadmin@college.edu")
                .password("adminpass")
                .college("Test College")
                .build();
        
        MvcResult adminResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        
        // Manually set admin role
        User adminUser = userRepository.findByEmail("newadmin@college.edu").get();
        adminUser.setRole(Role.ADMIN);
        userRepository.save(adminUser);
        
        AuthResponse adminAuth = objectMapper.readValue(
                adminResult.getResponse().getContentAsString(), AuthResponse.class);
        adminToken = adminAuth.getToken();
    }
    
    @Test
    void testCreateReport() throws Exception {
        CreateReportRequest request = CreateReportRequest.builder()
                .listingId(listing.getId())
                .reason("This listing looks suspicious and might be a scam")
                .build();
        
        mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.listingId").value(listing.getId()))
                .andExpect(jsonPath("$.listingTitle").value("Suspicious Listing"))
                .andExpect(jsonPath("$.reporterId").value(reporter.getId()))
                .andExpect(jsonPath("$.reason").value("This listing looks suspicious and might be a scam"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.reportedAt").exists());
    }
    
    @Test
    void testCreateReportWithoutAuth() throws Exception {
        CreateReportRequest request = CreateReportRequest.builder()
                .listingId(listing.getId())
                .reason("Suspicious")
                .build();
        
        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void testCreateDuplicateReport() throws Exception {
        CreateReportRequest request = CreateReportRequest.builder()
                .listingId(listing.getId())
                .reason("Suspicious")
                .build();
        
        // First report
        mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
        
        // Duplicate report
        mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You have already reported this listing"));
    }
    
    @Test
    void testGetAllReportsAsAdmin() throws Exception {
        // Create reports
        Report report1 = Report.builder()
                .listing(listing)
                .reporter(reporter)
                .reason("Suspicious")
                .status(ReportStatus.PENDING)
                .build();
        reportRepository.save(report1);
        
        Report report2 = Report.builder()
                .listing(listing)
                .reporter(seller)
                .reason("Another report")
                .status(ReportStatus.PENDING)
                .build();
        reportRepository.save(report2);
        
        mockMvc.perform(get("/api/reports")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }
    
    @Test
    void testGetAllReportsAsStudent() throws Exception {
        mockMvc.perform(get("/api/reports")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }
    
    @Test
    void testGetPendingReports() throws Exception {
        // Create pending report
        Report pendingReport = Report.builder()
                .listing(listing)
                .reporter(reporter)
                .reason("Pending")
                .status(ReportStatus.PENDING)
                .build();
        reportRepository.save(pendingReport);
        
        // Create resolved report
        Report resolvedReport = Report.builder()
                .listing(listing)
                .reporter(seller)
                .reason("Already resolved")
                .status(ReportStatus.RESOLVED)
                .build();
        reportRepository.save(resolvedReport);
        
        mockMvc.perform(get("/api/reports/pending")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }
    
    @Test
    void testResolveReport() throws Exception {
        Report report = Report.builder()
                .listing(listing)
                .reporter(reporter)
                .reason("Scam")
                .status(ReportStatus.PENDING)
                .build();
        report = reportRepository.save(report);
        
        mockMvc.perform(patch("/api/reports/" + report.getId() + "/resolve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolvedAt").exists());
        
        // Verify listing is removed
        mockMvc.perform(get("/api/listings/" + listing.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REMOVED"));
    }
    
    @Test
    void testDismissReport() throws Exception {
        Report report = Report.builder()
                .listing(listing)
                .reporter(reporter)
                .reason("False alarm")
                .status(ReportStatus.PENDING)
                .build();
        report = reportRepository.save(report);
        
        mockMvc.perform(patch("/api/reports/" + report.getId() + "/dismiss")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISMISSED"))
                .andExpect(jsonPath("$.resolvedAt").exists());
        
        // Verify listing is still available
        mockMvc.perform(get("/api/listings/" + listing.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }
    
    @Test
    void testResolveReportAsStudent() throws Exception {
        Report report = Report.builder()
                .listing(listing)
                .reporter(reporter)
                .reason("Scam")
                .status(ReportStatus.PENDING)
                .build();
        report = reportRepository.save(report);
        
        mockMvc.perform(patch("/api/reports/" + report.getId() + "/resolve")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }
}
