package com.omar.incident_monitoring.dto;

import java.time.LocalDateTime;

import com.omar.incident_monitoring.entity.Metric.MetricType;
import com.omar.incident_monitoring.entity.MetricThreshold.ThresholdOperator;

public record MetricThresholdResponse(
        Long id,
        Long serviceId,
        MetricType metricType,
        ThresholdOperator operator,
        double thresholdValue,
        boolean enabled,
        LocalDateTime createdAt
) {}
