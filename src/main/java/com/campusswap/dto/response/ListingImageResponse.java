package com.campusswap.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingImageResponse {
    private Long id;
    private String imageUrl;
    private Boolean isPrimary;
    private LocalDateTime createdAt;
}
