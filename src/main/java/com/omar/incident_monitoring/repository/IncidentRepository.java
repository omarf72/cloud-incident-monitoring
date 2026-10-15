package com.omar.incident_monitoring.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.omar.incident_monitoring.entity.Incident;


public interface  IncidentRepository extends JpaRepository<Incident,Long> {

     List<Incident> findAllByApplicationId(Long applicationId);
    List<Incident> findAllByServiceId(Long serviceId);
    
}
