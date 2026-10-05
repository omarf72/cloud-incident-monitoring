package com.omar.incident_monitoring.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity 
@Table(name="incidents")
public class Incident {
    
    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "application_id",nullable = false)
    private Application application;

    @ManyToOne 
    @JoinColumn (name="service_id")
    private Service service;

    private String name;

    private String severity;

    private String status;

    private String reason;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name="ended_at")
    private LocalDateTime endedAt;

    @Column(name="created_at")
    private LocalDateTime createdAt;

}
