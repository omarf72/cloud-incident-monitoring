package com.omar.incident_monitoring.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import com.omar.incident_monitoring.entity.Metric.MetricType;

@Entity
@Table(name = "metric_thresholds")
public class MetricThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    public enum ThresholdOperator {
        GREATER_THAN,
        LESS_THAN
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false, length = 100)
    private MetricType metricType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ThresholdOperator operator;

    @Column(name = "threshold_value", nullable = false)
    private double thresholdValue;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public MetricThreshold() {
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public MetricType getMetricType() {
        return metricType;
    }

    public void setMetricType(MetricType metricType) {
        this.metricType = metricType;
    }

    public ThresholdOperator getOperator() {
        return operator;
    }

    public void setOperator(ThresholdOperator operator) {
        this.operator = operator;
    }

    public double getThresholdValue() {
        return thresholdValue;
    }

    public void setThresholdValue(double thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
