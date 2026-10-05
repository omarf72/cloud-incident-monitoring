package com.omar.incident_monitoring.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity 
@Table(name="metrics")
public class Metric {

    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)

    private Long id;

    @ManyToOne 
    @JoinColumn (name="application_id",nullable = false)
    private Application application;

    @ManyToOne
    @JoinColumn(name = "service_id")
    private Service service;

    @ManyToOne 
    @JoinColumn (name = "experiment_id")
    private Experiment experiment;

    private String type;

    private double value;

    @Column (name = "recorded_at")
    private LocalDateTime recordedAt;

}
