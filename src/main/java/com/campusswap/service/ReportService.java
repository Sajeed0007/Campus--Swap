package com.campusswap.service;

import com.campusswap.dto.request.CreateReportRequest;
import com.campusswap.dto.response.ReportResponse;
import com.campusswap.exception.BadRequestException;
import com.campusswap.exception.ResourceNotFoundException;
import com.campusswap.mapper.ReportMapper;
import com.campusswap.model.Listing;
import com.campusswap.model.Report;
import com.campusswap.model.User;
import com.campusswap.model.enums.ReportStatus;
import com.campusswap.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {
    
    private final ReportRepository reportRepository;
    private final UserService userService;
    private final ListingService listingService;
    private final ReportMapper reportMapper;
    
    @Transactional
    public ReportResponse createReport(Long reporterId, CreateReportRequest request) {
        log.info("Creating report for listing ID: {} by user ID: {}", request.getListingId(), reporterId);
        
        if (reportRepository.existsByReporterIdAndListingId(reporterId, request.getListingId())) {
            throw new BadRequestException("You have already reported this listing");
        }
        
        User reporter = userService.findUserById(reporterId);
        Listing listing = listingService.findListingById(request.getListingId());
        
        Report report = Report.builder()
                .listing(listing)
                .reporter(reporter)
                .reason(request.getReason())
                .build();
        
        Report savedReport = reportRepository.save(report);
        log.info("Report created successfully with ID: {}", savedReport.getId());
        
        return reportMapper.toResponse(savedReport);
    }
    
    @Transactional(readOnly = true)
    public List<ReportResponse> getAllReports() {
        log.info("Fetching all reports");
        
        List<Report> reports = reportRepository.findAll();
        return reports.stream()
                .map(reportMapper::toResponse)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<ReportResponse> getPendingReports() {
        log.info("Fetching pending reports");
        
        List<Report> reports = reportRepository.findByStatus(ReportStatus.PENDING);
        return reports.stream()
                .map(reportMapper::toResponse)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<ReportResponse> getReportsByListing(Long listingId) {
        log.info("Fetching reports for listing ID: {}", listingId);
        
        List<Report> reports = reportRepository.findByListingId(listingId);
        return reports.stream()
                .map(reportMapper::toResponse)
                .toList();
    }
    
    @Transactional
    public ReportResponse resolveReport(Long reportId) {
        log.info("Resolving report ID: {}", reportId);
        
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", "id", reportId));
        
        report.setStatus(ReportStatus.RESOLVED);
        report.setResolvedAt(LocalDateTime.now());
        
        // Remove the listing
        listingService.removeListing(report.getListing().getId());
        
        Report resolvedReport = reportRepository.save(report);
        log.info("Report resolved successfully with ID: {}", reportId);
        
        return reportMapper.toResponse(resolvedReport);
    }
    
    @Transactional
    public ReportResponse dismissReport(Long reportId) {
        log.info("Dismissing report ID: {}", reportId);
        
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", "id", reportId));
        
        report.setStatus(ReportStatus.DISMISSED);
        report.setResolvedAt(LocalDateTime.now());
        
        Report dismissedReport = reportRepository.save(report);
        log.info("Report dismissed successfully with ID: {}", reportId);
        
        return reportMapper.toResponse(dismissedReport);
    }
}
