package com.campusswap.mapper;

import com.campusswap.dto.response.WishlistResponse;
import com.campusswap.model.Wishlist;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WishlistMapper {
    
    private final ListingMapper listingMapper;
    
    public WishlistResponse toResponse(Wishlist wishlist) {
        if (wishlist == null) {
            return null;
        }
        
        return WishlistResponse.builder()
                .id(wishlist.getId())
                .listing(listingMapper.toSummaryResponse(wishlist.getListing()))
                .addedAt(wishlist.getAddedAt())
                .build();
    }
}
