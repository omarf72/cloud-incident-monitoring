package com.omar.incident_monitoring.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
@Table(name = "metrics")
public class Metric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long id;

    @ManyToOne
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @ManyToOne
    @JoinColumn(name = "service_id")
    private Service service;

    @ManyToOne
    @JoinColumn(name = "experiment_id")
    private Experiment experiment;

    public enum MetricType {

        // System resources
        CPU_USAGE_PERCENT,
        MEMORY_USAGE_PERCENT,
        MEMORY_USED_MB,
        MEMORY_AVAILABLE_MB,
        DISK_USAGE_PERCENT,
        DISK_USED_MB,
        DISK_AVAILABLE_MB,
        DISK_READ_BYTES_PER_SECOND,
        DISK_WRITE_BYTES_PER_SECOND,
        NETWORK_RECEIVED_BYTES_PER_SECOND,
        NETWORK_SENT_BYTES_PER_SECOND,
        SYSTEM_LOAD_AVERAGE,
        THREAD_COUNT,
        OPEN_FILE_DESCRIPTORS,

        // Application performance
        LATENCY_MS,
        LATENCY_P50_MS,
        LATENCY_P95_MS,
        LATENCY_P99_MS,
        REQUEST_COUNT,
        REQUESTS_PER_SECOND,
        ERROR_COUNT,
        ERROR_RATE_PERCENT,
        SUCCESS_RATE_PERCENT,
        TIMEOUT_COUNT,
        CONCURRENT_REQUESTS,
        RESPONSE_SIZE_BYTES,

        // Database health
        DB_CONNECTIONS_ACTIVE,
        DB_CONNECTIONS_IDLE,
        DB_CONNECTION_POOL_USAGE_PERCENT,
        DB_QUERY_LATENCY_MS,
        DB_QUERY_COUNT,
        DB_SLOW_QUERY_COUNT,
        DB_TRANSACTION_COUNT,
        DB_DEADLOCK_COUNT,
        DB_CACHE_HIT_RATE_PERCENT,
        DB_REPLICATION_LAG_MS,

        // Availability and reliability
        HEALTH_CHECK_STATUS,
        UPTIME_SECONDS,
        AVAILABILITY_PERCENT,
        RESTART_COUNT,
        FAILED_HEALTH_CHECK_COUNT,
        DEPENDENCY_FAILURE_COUNT,
        CIRCUIT_BREAKER_OPEN_COUNT,

        // Traffic and scaling
        INBOUND_REQUESTS_PER_SECOND,
        OUTBOUND_REQUESTS_PER_SECOND,
        QUEUE_DEPTH,
        QUEUE_WAIT_TIME_MS,
        JOBS_PROCESSED_COUNT,
        JOBS_FAILED_COUNT,
        AUTOSCALING_INSTANCE_COUNT,
        ACTIVE_INSTANCE_COUNT
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MetricType type;

    private double value;

    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public Experiment getExperiment() {
        return experiment;
    }

    public void setExperiment(Experiment experiment) {
        this.experiment = experiment;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }

    public MetricType getType() {
        return type;
    }

    public void setType(MetricType type) {
        this.type = type;
    }

}
