package com.omar.incident_monitoring.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.omar.incident_monitoring.entity.Application;
import java.util.List;
import java.util.Optional;

public interface  ApplicationRepository 
extends JpaRepository<Application,Long>{ 

    List<Application> findAllByUserId(Long userId);


    Optional<Application> findByIdAndUserId(Long id,Long userId);

    
}
