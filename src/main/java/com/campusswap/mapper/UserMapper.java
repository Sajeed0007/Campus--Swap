package com.campusswap.mapper;

import com.campusswap.dto.response.SellerInfoResponse;
import com.campusswap.dto.response.UserResponse;
import com.campusswap.model.User;
import com.campusswap.model.enums.ListingStatus;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    
    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        
        int activeListingsCount = (int) user.getListings().stream()
                .filter(listing -> listing.getStatus() == ListingStatus.AVAILABLE)
                .count();
        
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .college(user.getCollege())
                .hostelOrDorm(user.getHostelOrDorm())
                .role(user.getRole())
                .activeListingsCount(activeListingsCount)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
    
    public SellerInfoResponse toSellerInfo(User user) {
        if (user == null) {
            return null;
        }
        
        return SellerInfoResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .hostelOrDorm(user.getHostelOrDorm())
                .college(user.getCollege())
                .build();
    }
}
