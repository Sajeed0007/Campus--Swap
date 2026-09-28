package com.campusswap.service;

import com.campusswap.dto.response.WishlistResponse;
import com.campusswap.exception.BadRequestException;
import com.campusswap.exception.ResourceNotFoundException;
import com.campusswap.mapper.WishlistMapper;
import com.campusswap.model.Listing;
import com.campusswap.model.User;
import com.campusswap.model.Wishlist;
import com.campusswap.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WishlistService {
    
    private final WishlistRepository wishlistRepository;
    private final UserService userService;
    private final ListingService listingService;
    private final WishlistMapper wishlistMapper;
    
    @Transactional
    public WishlistResponse addToWishlist(Long userId, Long listingId) {
        log.info("Adding listing ID: {} to wishlist for user ID: {}", listingId, userId);
        
        if (wishlistRepository.existsByUserIdAndListingId(userId, listingId)) {
            throw new BadRequestException("Listing already in wishlist");
        }
        
        User user = userService.findUserById(userId);
        Listing listing = listingService.findListingById(listingId);
        
        Wishlist wishlist = Wishlist.builder()
                .user(user)
                .listing(listing)
                .build();
        
        Wishlist savedWishlist = wishlistRepository.save(wishlist);
        log.info("Listing added to wishlist successfully");
        
        return wishlistMapper.toResponse(savedWishlist);
    }
    
    @Transactional(readOnly = true)
    public List<WishlistResponse> getUserWishlist(Long userId) {
        log.info("Fetching wishlist for user ID: {}", userId);
        
        List<Wishlist> wishlists = wishlistRepository.findByUserId(userId);
        return wishlists.stream()
                .map(wishlistMapper::toResponse)
                .toList();
    }
    
    @Transactional
    public void removeFromWishlist(Long userId, Long listingId) {
        log.info("Removing listing ID: {} from wishlist for user ID: {}", listingId, userId);
        
        if (!wishlistRepository.existsByUserIdAndListingId(userId, listingId)) {
            throw new ResourceNotFoundException("Wishlist entry not found");
        }
        
        wishlistRepository.deleteByUserIdAndListingId(userId, listingId);
        log.info("Listing removed from wishlist successfully");
    }
    
    @Transactional(readOnly = true)
    public boolean isInWishlist(Long userId, Long listingId) {
        return wishlistRepository.existsByUserIdAndListingId(userId, listingId);
    }
}
