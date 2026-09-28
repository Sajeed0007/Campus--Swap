package com.campusswap.repository;

import com.campusswap.model.ListingImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ListingImageRepository extends JpaRepository<ListingImage, Long> {
    
    List<ListingImage> findByListingId(Long listingId);
    
    void deleteByListingId(Long listingId);
}
