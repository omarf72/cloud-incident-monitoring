package com.omar.incident_monitoring.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity 
@Table(name="experiments")
public class Experiment {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long id;

    @ManyToOne
    @JoinColumn(name="application_id",nullable = false)
    private Application application;

    @ManyToOne
    @JoinColumn(name = "service_id")
    private Service service;

    private String type;

    private String description;


    @Column(name="planned_duration_seconds")

    private Integer plannedDurationSeconds;

    @Column(name="started_at")
    private LocalDateTime startedAt;

    @Column(name="ended_at")
    private LocalDateTime endedAt;

    private String status;

    @Column(name="end_reason")
    private String endReason;

    @Column(name="created_at")
    private LocalDateTime createdAt;


}
