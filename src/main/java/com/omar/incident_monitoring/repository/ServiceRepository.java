package com.omar.incident_monitoring.repository;

import com.omar.incident_monitoring.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceRepository extends JpaRepository<Service, Long> {
    List<Service> findAllByApplicationId(Long applicationId);


Optional<Service> findByIdAndApplicationId(Long id, Long applicationId);
}


