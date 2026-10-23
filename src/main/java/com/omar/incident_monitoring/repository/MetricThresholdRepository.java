package com.omar.incident_monitoring.repository;

import com.omar.incident_monitoring.entity.MetricThreshold;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MetricThresholdRepository
        extends JpaRepository<MetricThreshold, Long> {

    List<MetricThreshold> findByService_Id(Long serviceId);
}
