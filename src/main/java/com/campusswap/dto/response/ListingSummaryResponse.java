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

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingSummaryResponse {
    private Long id;
    private String title;
    private BigDecimal price;
    private Category category;
    private TransactionType transactionType;
    private ItemCondition itemCondition;
    private ListingStatus status;
    private String primaryImageUrl;
    private LocalDateTime createdAt;
}
