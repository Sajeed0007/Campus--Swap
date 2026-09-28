package com.campusswap.controller;

import com.campusswap.dto.response.WishlistResponse;
import com.campusswap.service.WishlistService;
import com.campusswap.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('STUDENT')")
public class WishlistController {
    
    private final WishlistService wishlistService;
    private final SecurityUtil securityUtil;
    
    @PostMapping("/{listingId}")
    public ResponseEntity<WishlistResponse> addToWishlist(@PathVariable Long listingId) {
        Long userId = securityUtil.getCurrentUserId();
        log.info("POST /api/wishlist/{} - Adding to wishlist for user {}", listingId, userId);
        WishlistResponse response = wishlistService.addToWishlist(userId, listingId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @GetMapping
    public ResponseEntity<List<WishlistResponse>> getMyWishlist() {
        Long userId = securityUtil.getCurrentUserId();
        log.info("GET /api/wishlist - Fetching wishlist for user {}", userId);
        List<WishlistResponse> response = wishlistService.getUserWishlist(userId);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{listingId}")
    public ResponseEntity<Void> removeFromWishlist(@PathVariable Long listingId) {
        Long userId = securityUtil.getCurrentUserId();
        log.info("DELETE /api/wishlist/{} - Removing from wishlist for user {}", listingId, userId);
        wishlistService.removeFromWishlist(userId, listingId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/check/{listingId}")
    public ResponseEntity<Boolean> isInWishlist(@PathVariable Long listingId) {
        Long userId = securityUtil.getCurrentUserId();
        log.info("GET /api/wishlist/check/{} - Checking wishlist for user {}", listingId, userId);
        boolean inWishlist = wishlistService.isInWishlist(userId, listingId);
        return ResponseEntity.ok(inWishlist);
    }
}
