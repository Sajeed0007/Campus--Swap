package com.campusswap.dto.response;

import com.campusswap.model.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private Long id;
    private String email;
    private String fullName;
    private String college;
    private Role role;
    private String token;

    /**
     * Without @Builder.Default, Lombok ignores this initializer when the builder is
     * used, so the field would serialize as null unless every call site sets it.
     */
    @Builder.Default
    private String tokenType = "Bearer";
}
