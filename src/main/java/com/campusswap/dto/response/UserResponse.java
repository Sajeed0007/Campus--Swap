package com.campusswap.dto.response;

import com.campusswap.model.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String college;
    private String hostelOrDorm;
    private Role role;
    private Integer activeListingsCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
