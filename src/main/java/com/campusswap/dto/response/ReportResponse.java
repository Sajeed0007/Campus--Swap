package com.campusswap.dto.response;

import com.campusswap.model.enums.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponse {
    private Long id;
    private Long listingId;
    private String listingTitle;
    private Long reporterId;
    private String reporterName;
    private String reason;
    private ReportStatus status;
    private LocalDateTime reportedAt;
    private LocalDateTime resolvedAt;
}
