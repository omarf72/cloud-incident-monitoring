package com.omar.incident_monitoring.dto;

import java.util.List;

public record MetricHistoryResponse(
                String key,
                String name,
                String range,
                String unit,
                List<MetricSample> samples) {
}
