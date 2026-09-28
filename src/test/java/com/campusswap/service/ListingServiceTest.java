package com.campusswap.service;

import com.campusswap.dto.request.UpdateListingRequest;
import com.campusswap.exception.ResourceNotFoundException;
import com.campusswap.exception.UnauthorizedException;
import com.campusswap.mapper.ListingMapper;
import com.campusswap.model.Listing;
import com.campusswap.model.User;
import com.campusswap.model.enums.Category;
import com.campusswap.model.enums.ItemCondition;
import com.campusswap.model.enums.ListingStatus;
import com.campusswap.model.enums.Role;
import com.campusswap.model.enums.TransactionType;
import com.campusswap.repository.ListingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the ownership rules in {@link ListingService}. These run without a
 * Spring context so the authorization branch is asserted in isolation from the
 * filter chain and method security.
 */
@ExtendWith(MockitoExtension.class)
class ListingServiceTest {

    private static final long OWNER_ID = 1L;
    private static final long INTRUDER_ID = 2L;
    private static final long LISTING_ID = 100L;

    @Mock private ListingRepository listingRepository;
    @Mock private UserService userService;
    @Mock private ListingMapper listingMapper;

    @InjectMocks private ListingService listingService;

    private Listing listing;

    @BeforeEach
    void setUp() {
        User owner = User.builder()
                .id(OWNER_ID)
                .email("owner@college.edu")
                .fullName("Owner")
                .college("Test College")
                .role(Role.STUDENT)
                .build();

        listing = Listing.builder()
                .id(LISTING_ID)
                .title("Original Title")
                .description("Original description")
                .price(new BigDecimal("25.00"))
                .category(Category.BOOKS)
                .transactionType(TransactionType.SELL)
                .itemCondition(ItemCondition.GOOD)
                .status(ListingStatus.AVAILABLE)
                .seller(owner)
                .build();
    }

    @Test
    @DisplayName("updating someone else's listing throws and does not persist")
    void updateRejectsNonOwner() {
        when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(listing));

        UpdateListingRequest request = UpdateListingRequest.builder()
                .title("Hijacked Title")
                .build();

        assertThrows(UnauthorizedException.class,
                () -> listingService.updateListing(LISTING_ID, INTRUDER_ID, request));

        verify(listingRepository, never()).save(any(Listing.class));
        assertEquals("Original Title", listing.getTitle(), "Entity must not be mutated");
    }

    @Test
    @DisplayName("deleting someone else's listing throws and does not delete")
    void deleteRejectsNonOwner() {
        when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(listing));

        assertThrows(UnauthorizedException.class,
                () -> listingService.deleteListing(LISTING_ID, INTRUDER_ID));

        verify(listingRepository, never()).delete(any(Listing.class));
    }

    @Test
    @DisplayName("marking someone else's listing sold throws and leaves status untouched")
    void markAsSoldRejectsNonOwner() {
        when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(listing));

        assertThrows(UnauthorizedException.class,
                () -> listingService.markAsSold(LISTING_ID, INTRUDER_ID));

        verify(listingRepository, never()).save(any(Listing.class));
        assertEquals(ListingStatus.AVAILABLE, listing.getStatus());
    }

    @Test
    @DisplayName("the owner can mark their own listing sold")
    void markAsSoldAllowsOwner() {
        when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));

        listingService.markAsSold(LISTING_ID, OWNER_ID);

        assertEquals(ListingStatus.SOLD, listing.getStatus());
        verify(listingRepository).save(listing);
    }

    @Test
    @DisplayName("a missing listing throws ResourceNotFoundException")
    void missingListingThrows() {
        when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> listingService.findListingById(LISTING_ID));
    }

    @Test
    @DisplayName("an admin takedown sets REMOVED without an ownership check")
    void removeListingMarksRemoved() {
        when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));

        listingService.removeListing(LISTING_ID);

        assertEquals(ListingStatus.REMOVED, listing.getStatus());
    }
}
