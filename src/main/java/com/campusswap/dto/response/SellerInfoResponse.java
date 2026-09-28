package com.campusswap.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerInfoResponse {
    private Long id;
    private String fullName;
    private String phoneNumber;
    private String hostelOrDorm;
    private String college;
}
