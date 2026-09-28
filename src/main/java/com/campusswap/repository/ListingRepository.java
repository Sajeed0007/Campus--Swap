package com.campusswap.repository;

import com.campusswap.model.Listing;
import com.campusswap.model.enums.Category;
import com.campusswap.model.enums.ListingStatus;
import com.campusswap.model.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long> {
    
    Page<Listing> findByStatus(ListingStatus status, Pageable pageable);
    
    Page<Listing> findByStatusAndCategory(ListingStatus status, Category category, Pageable pageable);
    
    Page<Listing> findByStatusAndTransactionType(ListingStatus status, TransactionType transactionType, Pageable pageable);
    
    Page<Listing> findByStatusAndCategoryAndTransactionType(
            ListingStatus status, 
            Category category, 
            TransactionType transactionType, 
            Pageable pageable
    );
    
    @Query("SELECT l FROM Listing l WHERE l.status = :status AND " +
           "(LOWER(l.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(l.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Listing> findByStatusAndSearchTerm(
            @Param("status") ListingStatus status,
            @Param("search") String search,
            Pageable pageable
    );
    
    @Query("SELECT l FROM Listing l WHERE l.status = :status AND " +
           "l.category = :category AND " +
           "(LOWER(l.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(l.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Listing> findByStatusAndCategoryAndSearchTerm(
            @Param("status") ListingStatus status,
            @Param("category") Category category,
            @Param("search") String search,
            Pageable pageable
    );
    
    @Query("SELECT l FROM Listing l WHERE l.status = :status AND " +
           "l.transactionType = :transactionType AND " +
           "(LOWER(l.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(l.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Listing> findByStatusAndTransactionTypeAndSearchTerm(
            @Param("status") ListingStatus status,
            @Param("transactionType") TransactionType transactionType,
            @Param("search") String search,
            Pageable pageable
    );
    
    @Query("SELECT l FROM Listing l WHERE l.status = :status AND " +
           "l.category = :category AND " +
           "l.transactionType = :transactionType AND " +
           "(LOWER(l.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(l.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Listing> findByStatusAndCategoryAndTransactionTypeAndSearchTerm(
            @Param("status") ListingStatus status,
            @Param("category") Category category,
            @Param("transactionType") TransactionType transactionType,
            @Param("search") String search,
            Pageable pageable
    );
    
    // Price filtering queries
    @Query("SELECT l FROM Listing l WHERE l.status = :status AND " +
           "(:minPrice IS NULL OR l.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR l.price <= :maxPrice)")
    Page<Listing> findByStatusAndPriceRange(
            @Param("status") ListingStatus status,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );
    
    @Query("SELECT l FROM Listing l WHERE l.status = :status AND " +
           "(:category IS NULL OR l.category = :category) AND " +
           "(:transactionType IS NULL OR l.transactionType = :transactionType) AND " +
           "(:minPrice IS NULL OR l.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR l.price <= :maxPrice) AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(l.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(l.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Listing> findByFilters(
            @Param("status") ListingStatus status,
            @Param("category") Category category,
            @Param("transactionType") TransactionType transactionType,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("search") String search,
            Pageable pageable
    );
    
    List<Listing> findBySellerId(Long sellerId);

    /**
     * Seller's publicly visible listings. Excludes moderated (REMOVED) listings so
     * content taken down by an admin is not reachable via the public profile route.
     */
    List<Listing> findBySellerIdAndStatusNotOrderByCreatedAtDesc(Long sellerId, ListingStatus status);
    
    Page<Listing> findBySellerIdAndStatus(Long sellerId, ListingStatus status, Pageable pageable);
    
    @Query("SELECT COUNT(l) FROM Listing l WHERE l.seller.id = :sellerId AND l.status = :status")
    long countBySellerIdAndStatus(@Param("sellerId") Long sellerId, @Param("status") ListingStatus status);
}
