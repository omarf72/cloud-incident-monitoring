package com.omar.incident_monitoring.repository;

import com.omar.incident_monitoring.entity.Metric.MetricType;
import com.omar.incident_monitoring.entity.MetricThreshold;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MetricThresholdRepository
        extends JpaRepository<MetricThreshold, Long> {

    List<MetricThreshold> findByService_Id(Long serviceId);

    List<MetricThreshold> findByService_IdAndMetricTypeAndEnabledTrue(
            Long serviceId,
            MetricType metricType);
    
    Optional<MetricThreshold> findByIdAndService_Id(
        Long thresholdId,
        Long serviceId);
}
