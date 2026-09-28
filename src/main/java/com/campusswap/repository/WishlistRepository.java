package com.campusswap.repository;

import com.campusswap.model.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    
    List<Wishlist> findByUserId(Long userId);
    
    Optional<Wishlist> findByUserIdAndListingId(Long userId, Long listingId);
    
    boolean existsByUserIdAndListingId(Long userId, Long listingId);
    
    void deleteByUserIdAndListingId(Long userId, Long listingId);
}
