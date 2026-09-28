package com.campusswap.controller;

import com.campusswap.dto.request.CreateListingRequest;
import com.campusswap.dto.request.UpdateListingRequest;
import com.campusswap.dto.response.ListingResponse;
import com.campusswap.dto.response.ListingSummaryResponse;
import com.campusswap.dto.response.PageResponse;
import com.campusswap.model.enums.Category;
import com.campusswap.model.enums.TransactionType;
import com.campusswap.service.ListingService;
import com.campusswap.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
@Slf4j
public class ListingController {
    
    private final ListingService listingService;
    private final SecurityUtil securityUtil;
    
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ListingResponse> createListing(@Valid @RequestBody CreateListingRequest request) {
        Long sellerId = securityUtil.getCurrentUserId();
        log.info("POST /api/listings - Creating listing for seller ID: {}", sellerId);
        ListingResponse response = listingService.createListing(sellerId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ListingResponse> getListingById(@PathVariable Long id) {
        log.info("GET /api/listings/{} - Fetching listing", id);
        ListingResponse response = listingService.getListingById(id);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    public ResponseEntity<PageResponse<ListingSummaryResponse>> getAllListings(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) TransactionType transactionType,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        log.info("GET /api/listings - Fetching all listings with filters");
        PageResponse<ListingSummaryResponse> response = listingService.getAllListings(
                search, category, transactionType, minPrice, maxPrice, page, size, sortBy, sortDirection
        );
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/my-listings")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<ListingResponse>> getMyListings() {
        Long userId = securityUtil.getCurrentUserId();
        log.info("GET /api/listings/my-listings - Fetching listings for user ID: {}", userId);
        List<ListingResponse> response = listingService.getUserListings(userId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ListingResponse>> getUserListings(@PathVariable Long userId) {
        log.info("GET /api/listings/user/{} - Fetching public user listings", userId);
        List<ListingResponse> response = listingService.getPublicUserListings(userId);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ListingResponse> updateListing(
            @PathVariable Long id,
            @Valid @RequestBody UpdateListingRequest request
    ) {
        Long userId = securityUtil.getCurrentUserId();
        log.info("PUT /api/listings/{} - Updating listing by user ID: {}", id, userId);
        ListingResponse response = listingService.updateListing(id, userId, request);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Void> deleteListing(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        log.info("DELETE /api/listings/{} - Deleting listing by user ID: {}", id, userId);
        listingService.deleteListing(id, userId);
        return ResponseEntity.noContent().build();
    }
    
    @PatchMapping("/{id}/mark-sold")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ListingResponse> markAsSold(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        log.info("PATCH /api/listings/{}/mark-sold - Marking as sold by user ID: {}", id, userId);
        ListingResponse response = listingService.markAsSold(id, userId);
        return ResponseEntity.ok(response);
    }
}
