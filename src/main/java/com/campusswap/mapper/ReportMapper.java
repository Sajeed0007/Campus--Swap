package com.campusswap.mapper;

import com.campusswap.dto.response.ReportResponse;
import com.campusswap.model.Report;
import org.springframework.stereotype.Component;

@Component
public class ReportMapper {
    
    public ReportResponse toResponse(Report report) {
        if (report == null) {
            return null;
        }
        
        return ReportResponse.builder()
                .id(report.getId())
                .listingId(report.getListing().getId())
                .listingTitle(report.getListing().getTitle())
                .reporterId(report.getReporter().getId())
                .reporterName(report.getReporter().getFullName())
                .reason(report.getReason())
                .status(report.getStatus())
                .reportedAt(report.getReportedAt())
                .resolvedAt(report.getResolvedAt())
                .build();
    }
}
