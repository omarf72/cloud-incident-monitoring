package com.omar.incident_monitoring.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.omar.incident_monitoring.entity.Metric;

public interface MetricRepository extends JpaRepository<Metric, Long> {

    List<Metric> findByApplicationIdAndServiceIdAndRecordedAtBetweenOrderByRecordedAtAsc(
            Long applicationId,
            Long serviceId,
            LocalDateTime start,
            LocalDateTime end);

    
    Optional<Metric> findTopByService_IdOrderByRecordedAtDesc(Long serviceId);
}
