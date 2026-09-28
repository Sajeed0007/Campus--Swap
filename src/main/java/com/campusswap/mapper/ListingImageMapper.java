package com.campusswap.mapper;

import com.campusswap.dto.response.ListingImageResponse;
import com.campusswap.model.ListingImage;
import org.springframework.stereotype.Component;

@Component
public class ListingImageMapper {
    
    public ListingImageResponse toResponse(ListingImage image) {
        if (image == null) {
            return null;
        }
        
        return ListingImageResponse.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .isPrimary(image.getIsPrimary())
                .createdAt(image.getCreatedAt())
                .build();
    }
}
