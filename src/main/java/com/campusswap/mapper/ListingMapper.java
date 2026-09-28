package com.campusswap.mapper;

import com.campusswap.dto.request.CreateListingRequest;
import com.campusswap.dto.response.ListingResponse;
import com.campusswap.dto.response.ListingSummaryResponse;
import com.campusswap.model.Listing;
import com.campusswap.model.ListingImage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ListingMapper {
    
    private final UserMapper userMapper;
    private final ListingImageMapper listingImageMapper;
    
    public Listing toEntity(CreateListingRequest request) {
        if (request == null) {
            return null;
        }
        
        return Listing.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(request.getCategory())
                .transactionType(request.getTransactionType())
                .itemCondition(request.getItemCondition())
                .build();
    }
    
    public ListingResponse toResponse(Listing listing) {
        if (listing == null) {
            return null;
        }
        
        return ListingResponse.builder()
                .id(listing.getId())
                .title(listing.getTitle())
                .description(listing.getDescription())
                .price(listing.getPrice())
                .category(listing.getCategory())
                .transactionType(listing.getTransactionType())
                .itemCondition(listing.getItemCondition())
                .status(listing.getStatus())
                .images(listing.getImages().stream()
                        .map(listingImageMapper::toResponse)
                        .toList())
                .seller(userMapper.toSellerInfo(listing.getSeller()))
                .createdAt(listing.getCreatedAt())
                .updatedAt(listing.getUpdatedAt())
                .build();
    }
    
    public ListingSummaryResponse toSummaryResponse(Listing listing) {
        if (listing == null) {
            return null;
        }
        
        String primaryImageUrl = listing.getImages().stream()
                .filter(ListingImage::getIsPrimary)
                .findFirst()
                .map(ListingImage::getImageUrl)
                .orElse(listing.getImages().isEmpty() ? null : 
                        listing.getImages().get(0).getImageUrl());
        
        return ListingSummaryResponse.builder()
                .id(listing.getId())
                .title(listing.getTitle())
                .price(listing.getPrice())
                .category(listing.getCategory())
                .transactionType(listing.getTransactionType())
                .itemCondition(listing.getItemCondition())
                .status(listing.getStatus())
                .primaryImageUrl(primaryImageUrl)
                .createdAt(listing.getCreatedAt())
                .build();
    }
}
