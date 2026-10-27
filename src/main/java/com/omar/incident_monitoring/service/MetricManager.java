package com.omar.incident_monitoring.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.omar.incident_monitoring.dto.MetricCreateRequest;
import com.omar.incident_monitoring.dto.MetricResponse;
import com.omar.incident_monitoring.entity.Application;
import com.omar.incident_monitoring.entity.Metric;
import com.omar.incident_monitoring.entity.Metric.MetricType;
import com.omar.incident_monitoring.exception.ApplicationNotFoundException;
import com.omar.incident_monitoring.exception.ServiceNotFoundException;
import com.omar.incident_monitoring.repository.ApplicationRepository;
import com.omar.incident_monitoring.repository.MetricRepository;
import com.omar.incident_monitoring.repository.ServiceRepository;
import java.util.Collection;
import com.omar.incident_monitoring.entity.MetricThreshold;
import com.omar.incident_monitoring.entity.Incident;
import com.omar.incident_monitoring.entity.Incident.IncidentStatus;
import com.omar.incident_monitoring.repository.MetricThresholdRepository;
import com.omar.incident_monitoring.repository.IncidentRepository;

@Service
public class MetricManager {

        private final ApplicationRepository applicationRepository;
        private final ServiceRepository serviceRepository;
        private final MetricRepository metricRepository;
        private final MetricThresholdRepository metricThresholdRepository;
        private final IncidentRepository incidentRepository;

        public MetricManager(
                        ApplicationRepository applicationRepository,
                        ServiceRepository serviceRepository,
                        MetricRepository metricRepository,
                        MetricThresholdRepository metricThresholdRepository,
                        IncidentRepository incidentRepository) {
                this.applicationRepository = applicationRepository;
                this.serviceRepository = serviceRepository;
                this.metricRepository = metricRepository;
                this.metricThresholdRepository = metricThresholdRepository;
                this.incidentRepository = incidentRepository;
        }

        @Transactional
        public MetricResponse createMetric(
                        Long applicationId,
                        Long serviceId,
                        Long userId,
                        MetricCreateRequest request) {

                Application application = applicationRepository
                                .findByIdAndUserId(applicationId, userId)
                                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));

                com.omar.incident_monitoring.entity.Service service = serviceRepository
                                .findByIdAndApplicationId(serviceId, applicationId)
                                .orElseThrow(() -> new ServiceNotFoundException(serviceId));

                MetricType type;
                try {
                        type = MetricType.valueOf(
                                        request.type().trim().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException exception) {
                        throw new IllegalArgumentException(
                                        "Unsupported metric type: " + request.type());
                }

                double value = request.value();

                validateMetric(type, value);

                Metric metric = new Metric();
                metric.setApplication(application);
                metric.setService(service);
                metric.setType(type);
                metric.setValue(value);
                metric.setRecordedAt(LocalDateTime.now());

                Metric savedMetric = metricRepository.save(metric);

                evaluateThresholds(savedMetric);

                return toResponse(savedMetric);
        }

        @Transactional(readOnly = true)
        public List<MetricResponse> getMetrics(
                        Long applicationId,
                        Long serviceId,
                        Long userId,
                        LocalDateTime start,
                        LocalDateTime end) {

                applicationRepository.findByIdAndUserId(applicationId, userId)
                                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));

                serviceRepository.findByIdAndApplicationId(serviceId, applicationId)
                                .orElseThrow(() -> new ServiceNotFoundException(serviceId));

                if (start.isAfter(end)) {
                        throw new IllegalArgumentException(
                                        "Start time must be before or equal to end time");
                }

                return metricRepository
                                .findByApplicationIdAndServiceIdAndRecordedAtBetweenOrderByRecordedAtAsc(
                                                applicationId, serviceId, start, end)
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        private void validateMetric(MetricType type, double value) {
                if (!Double.isFinite(value) || value < 0) {
                        throw new IllegalArgumentException(
                                        "Metric value must be a finite, nonnegative number");
                }

                switch (type) {
                        // Percentage values must be between 0 and 100.
                        case CPU_USAGE_PERCENT,
                                        MEMORY_USAGE_PERCENT,
                                        DISK_USAGE_PERCENT,
                                        ERROR_RATE_PERCENT,
                                        SUCCESS_RATE_PERCENT,
                                        DB_CONNECTION_POOL_USAGE_PERCENT,
                                        DB_CACHE_HIT_RATE_PERCENT,
                                        AVAILABILITY_PERCENT -> {
                                if (value > 100) {
                                        throw new IllegalArgumentException(
                                                        type + " must be between 0 and 100");
                                }
                        }

                        // Count-based metrics must be whole numbers.
                        case THREAD_COUNT,
                                        OPEN_FILE_DESCRIPTORS,
                                        REQUEST_COUNT,
                                        ERROR_COUNT,
                                        TIMEOUT_COUNT,
                                        CONCURRENT_REQUESTS,
                                        DB_CONNECTIONS_ACTIVE,
                                        DB_CONNECTIONS_IDLE,
                                        DB_QUERY_COUNT,
                                        DB_SLOW_QUERY_COUNT,
                                        DB_TRANSACTION_COUNT,
                                        DB_DEADLOCK_COUNT,
                                        RESTART_COUNT,
                                        FAILED_HEALTH_CHECK_COUNT,
                                        DEPENDENCY_FAILURE_COUNT,
                                        CIRCUIT_BREAKER_OPEN_COUNT,
                                        QUEUE_DEPTH,
                                        JOBS_PROCESSED_COUNT,
                                        JOBS_FAILED_COUNT,
                                        AUTOSCALING_INSTANCE_COUNT,
                                        ACTIVE_INSTANCE_COUNT -> {
                                if (value != Math.floor(value)) {
                                        throw new IllegalArgumentException(
                                                        type + " must be a whole number");
                                }
                        }

                        // Status is categorical, so numeric values are not suitable.
                        case HEALTH_CHECK_STATUS ->
                                throw new IllegalArgumentException(
                                                "HEALTH_CHECK_STATUS requires a separate status representation");

                        default -> {
                                // Other numeric metrics allow nonnegative decimal values.
                        }

                }
        }

        private MetricResponse toResponse(Metric metric) {
                return new MetricResponse(
                                metric.getId(),
                                metric.getApplication().getId(),
                                metric.getService().getId(),
                                metric.getType().name(),
                                metric.getValue(),
                                metric.getRecordedAt());
        }

        private void evaluateThresholds(Metric metric) {

                List<MetricThreshold> thresholds = metricThresholdRepository
                                .findByService_IdAndMetricTypeAndEnabledTrue(
                                                metric.getService().getId(),
                                                metric.getType());

                for (MetricThreshold threshold : thresholds) {
                        double value = metric.getValue();
                        double limit = threshold.getThresholdValue();

                        boolean breached = switch (threshold.getOperator()) {
                                case GREATER_THAN -> value > limit;
                                case LESS_THAN -> value < limit;
                        };

                        if (!breached) {
                                continue;
                        }

                        boolean activeIncidentExists = incidentRepository
                                .findFirstByThreshold_IdAndStatusIn(
                                        threshold.getId(),
                                        List.of(
                                                IncidentStatus.OPEN,
                                                IncidentStatus.ACKNOWLEDGED))
                                        .isPresent();

                        if (activeIncidentExists) {
                                continue;
                        }

                        Incident incident = new Incident();
                        incident.setApplication(metric.getApplication());
                        incident.setService(metric.getService());
                        incident.setThreshold(threshold);
                        incident.setName(threshold.getMetricType()
                                        + " Threshold Breached");
                        incident.setSeverity(Incident.IncidentSeverity.HIGH);
                        incident.setStatus(IncidentStatus.OPEN);
                        incident.setReason(
                                        threshold.getMetricType() + " recorded " + value
                                                        + ", breaching the configured "
                                                        + threshold.getOperator() + " threshold of " + limit + ".");
                        incident.setStartedAt(metric.getRecordedAt());
                        incident.setCreatedAt(LocalDateTime.now());

                        incidentRepository.save(incident);
                }
        }
}
