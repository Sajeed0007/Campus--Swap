package com.campusswap.dto.response;

import com.campusswap.model.enums.Category;
import com.campusswap.model.enums.ItemCondition;
import com.campusswap.model.enums.ListingStatus;
import com.campusswap.model.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingResponse {
    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private Category category;
    private TransactionType transactionType;
    private ItemCondition itemCondition;
    private ListingStatus status;
    private List<ListingImageResponse> images;
    private SellerInfoResponse seller;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
