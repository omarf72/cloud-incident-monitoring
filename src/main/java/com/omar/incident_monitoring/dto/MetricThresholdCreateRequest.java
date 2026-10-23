package com.omar.incident_monitoring.dto;


import com.omar.incident_monitoring.entity.Metric.MetricType;
import com.omar.incident_monitoring.entity.MetricThreshold.ThresholdOperator;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record MetricThresholdCreateRequest(

        @NotNull
        MetricType metricType,

        @NotNull
        ThresholdOperator operator,

        @NotNull
        @PositiveOrZero
        Double thresholdValue

) {}
