package com.campusswap.controller;

import com.campusswap.dto.request.UpdateUserRequest;
import com.campusswap.dto.response.UserResponse;
import com.campusswap.exception.UnauthorizedException;
import com.campusswap.service.UserService;
import com.campusswap.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    
    private final UserService userService;
    private final SecurityUtil securityUtil;
    
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getCurrentUser() {
        log.info("GET /api/users/me - Fetching current user");
        Long userId = securityUtil.getCurrentUserId();
        UserResponse response = userService.getUserById(userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * UserResponse carries email, phone number and dorm. Allowing any authenticated
     * caller to read it by id would let a single student enumerate ids and harvest
     * contact details for the whole campus, so access is limited to self or admin.
     * Seller contact details for a specific listing remain available through
     * ListingResponse.seller.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        log.info("GET /api/users/{} - Fetching user", id);

        Long callerId = securityUtil.getCurrentUserId();
        boolean isSelf = id.equals(callerId);
        if (!isSelf && !SecurityUtil.hasRole("ADMIN")) {
            throw new UnauthorizedException("You may only view your own profile.");
        }

        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> updateCurrentUser(@Valid @RequestBody UpdateUserRequest request) {
        log.info("PUT /api/users/me - Updating current user");
        Long userId = securityUtil.getCurrentUserId();
        UserResponse response = userService.updateUser(userId, request);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("DELETE /api/users/{} - Deleting user (admin)", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
