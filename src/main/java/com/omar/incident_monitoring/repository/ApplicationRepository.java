package com.omar.incident_monitoring.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.omar.incident_monitoring.entity.Application;

public interface  ApplicationRepository 
extends JpaRepository<Application,Long>{ 
    
}
