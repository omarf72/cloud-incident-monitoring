package com.omar.incident_monitoring.dto;

import java.util.List;

public record IncidentPageResponse(
        List<IncidentResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {
}
