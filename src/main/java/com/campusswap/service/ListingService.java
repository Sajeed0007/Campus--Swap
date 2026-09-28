package com.campusswap.service;

import com.campusswap.dto.request.CreateListingRequest;
import com.campusswap.dto.request.UpdateListingRequest;
import com.campusswap.dto.response.ListingResponse;
import com.campusswap.dto.response.ListingSummaryResponse;
import com.campusswap.dto.response.PageResponse;
import com.campusswap.exception.ResourceNotFoundException;
import com.campusswap.exception.UnauthorizedException;
import com.campusswap.mapper.ListingMapper;
import com.campusswap.model.Listing;
import com.campusswap.model.User;
import com.campusswap.model.enums.Category;
import com.campusswap.model.enums.ListingStatus;
import com.campusswap.model.enums.TransactionType;
import com.campusswap.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ListingService {
    
    private final ListingRepository listingRepository;
    private final UserService userService;
    private final ListingMapper listingMapper;
    
    @Transactional
    public ListingResponse createListing(Long sellerId, CreateListingRequest request) {
        log.info("Creating listing for seller ID: {}", sellerId);
        
        User seller = userService.findUserById(sellerId);
        
        Listing listing = listingMapper.toEntity(request);
        listing.setSeller(seller);
        
        Listing savedListing = listingRepository.save(listing);
        log.info("Listing created successfully with ID: {}", savedListing.getId());
        
        return listingMapper.toResponse(savedListing);
    }
    
    @Transactional(readOnly = true)
    public ListingResponse getListingById(Long id) {
        log.info("Fetching listing with ID: {}", id);
        
        Listing listing = findListingById(id);
        return listingMapper.toResponse(listing);
    }
    
    @Transactional(readOnly = true)
    public PageResponse<ListingSummaryResponse> getAllListings(
            String search,
            Category category,
            TransactionType transactionType,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        log.info("Fetching listings - page: {}, size: {}, search: {}, category: {}, transactionType: {}, minPrice: {}, maxPrice: {}",
                page, size, search, category, transactionType, minPrice, maxPrice);
        
        Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC") ? 
                Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<Listing> listingPage = listingRepository.findByFilters(
                ListingStatus.AVAILABLE,
                category,
                transactionType,
                minPrice,
                maxPrice,
                search,
                pageable
        );
        
        List<ListingSummaryResponse> content = listingPage.getContent().stream()
                .map(listingMapper::toSummaryResponse)
                .toList();
        
        return PageResponse.<ListingSummaryResponse>builder()
                .content(content)
                .page(listingPage.getNumber())
                .size(listingPage.getSize())
                .totalElements(listingPage.getTotalElements())
                .totalPages(listingPage.getTotalPages())
                .first(listingPage.isFirst())
                .last(listingPage.isLast())
                .build();
    }
    
    /**
     * Owner view: returns every listing regardless of status, including SOLD and
     * REMOVED, so the seller can see the full history of their own items.
     */
    @Transactional(readOnly = true)
    public List<ListingResponse> getUserListings(Long userId) {
        log.info("Fetching own listings for user ID: {}", userId);
        
        List<Listing> listings = listingRepository.findBySellerId(userId);
        return listings.stream()
                .map(listingMapper::toResponse)
                .toList();
    }

    /**
     * Public view of another student's listings. Excludes REMOVED listings so
     * admin-moderated content stays unreachable.
     */
    @Transactional(readOnly = true)
    public List<ListingResponse> getPublicUserListings(Long userId) {
        log.info("Fetching public listings for user ID: {}", userId);

        List<Listing> listings = listingRepository
                .findBySellerIdAndStatusNotOrderByCreatedAtDesc(userId, ListingStatus.REMOVED);
        return listings.stream()
                .map(listingMapper::toResponse)
                .toList();
    }
    
    @Transactional
    public ListingResponse updateListing(Long listingId, Long userId, UpdateListingRequest request) {
        log.info("Updating listing ID: {} by user ID: {}", listingId, userId);
        
        Listing listing = findListingById(listingId);
        
        if (!listing.getSeller().getId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to update this listing");
        }
        
        if (request.getTitle() != null) {
            listing.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            listing.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            listing.setPrice(request.getPrice());
        }
        if (request.getCategory() != null) {
            listing.setCategory(request.getCategory());
        }
        if (request.getTransactionType() != null) {
            listing.setTransactionType(request.getTransactionType());
        }
        if (request.getItemCondition() != null) {
            listing.setItemCondition(request.getItemCondition());
        }
        
        Listing updatedListing = listingRepository.save(listing);
        log.info("Listing updated successfully with ID: {}", updatedListing.getId());
        
        return listingMapper.toResponse(updatedListing);
    }
    
    @Transactional
    public void deleteListing(Long listingId, Long userId) {
        log.info("Deleting listing ID: {} by user ID: {}", listingId, userId);
        
        Listing listing = findListingById(listingId);
        
        if (!listing.getSeller().getId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to delete this listing");
        }
        
        listingRepository.delete(listing);
        log.info("Listing deleted successfully with ID: {}", listingId);
    }
    
    @Transactional
    public ListingResponse markAsSold(Long listingId, Long userId) {
        log.info("Marking listing ID: {} as sold by user ID: {}", listingId, userId);
        
        Listing listing = findListingById(listingId);
        
        if (!listing.getSeller().getId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to mark this listing as sold");
        }
        
        listing.setStatus(ListingStatus.SOLD);
        Listing updatedListing = listingRepository.save(listing);
        
        log.info("Listing marked as sold successfully with ID: {}", listingId);
        return listingMapper.toResponse(updatedListing);
    }
    
    @Transactional
    public void removeListing(Long listingId) {
        log.info("Removing listing ID: {} (admin action)", listingId);
        
        Listing listing = findListingById(listingId);
        listing.setStatus(ListingStatus.REMOVED);
        listingRepository.save(listing);
        
        log.info("Listing removed successfully with ID: {}", listingId);
    }
    
    @Transactional(readOnly = true)
    public Listing findListingById(Long id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", "id", id));
    }
}
