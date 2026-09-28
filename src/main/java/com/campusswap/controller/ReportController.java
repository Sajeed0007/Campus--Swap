package com.campusswap.controller;

import com.campusswap.dto.request.CreateReportRequest;
import com.campusswap.dto.response.ReportResponse;
import com.campusswap.service.ReportService;
import com.campusswap.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {
    
    private final ReportService reportService;
    private final SecurityUtil securityUtil;
    
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ReportResponse> createReport(@Valid @RequestBody CreateReportRequest request) {
        Long reporterId = securityUtil.getCurrentUserId();
        log.info("POST /api/reports - Creating report by user ID: {}", reporterId);
        ReportResponse response = reportService.createReport(reporterId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReportResponse>> getAllReports() {
        log.info("GET /api/reports - Fetching all reports (admin)");
        List<ReportResponse> response = reportService.getAllReports();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReportResponse>> getPendingReports() {
        log.info("GET /api/reports/pending - Fetching pending reports (admin)");
        List<ReportResponse> response = reportService.getPendingReports();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/listing/{listingId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReportResponse>> getReportsByListing(@PathVariable Long listingId) {
        log.info("GET /api/reports/listing/{} - Fetching reports for listing", listingId);
        List<ReportResponse> response = reportService.getReportsByListing(listingId);
        return ResponseEntity.ok(response);
    }
    
    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportResponse> resolveReport(@PathVariable Long id) {
        log.info("PATCH /api/reports/{}/resolve - Resolving report (admin)", id);
        ReportResponse response = reportService.resolveReport(id);
        return ResponseEntity.ok(response);
    }
    
    @PatchMapping("/{id}/dismiss")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportResponse> dismissReport(@PathVariable Long id) {
        log.info("PATCH /api/reports/{}/dismiss - Dismissing report (admin)", id);
        ReportResponse response = reportService.dismissReport(id);
        return ResponseEntity.ok(response);
    }
}
