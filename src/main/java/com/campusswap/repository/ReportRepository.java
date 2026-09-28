package com.campusswap.repository;

import com.campusswap.model.Report;
import com.campusswap.model.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    
    List<Report> findByStatus(ReportStatus status);
    
    List<Report> findByListingId(Long listingId);
    
    List<Report> findByReporterId(Long reporterId);
    
    boolean existsByReporterIdAndListingId(Long reporterId, Long listingId);
}
