package com.omar.incident_monitoring.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity 
@Table (name="services")
public class Service {
    
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)

    private Long id;

    @ManyToOne
    @JoinColumn (name="application_id",nullable = false) 
    private Application application;

    private String name;

    private String type;

    @Column(name="created_at")
    private LocalDateTime createdAt;



}
