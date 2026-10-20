package com.omar.incident_monitoring.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.omar.incident_monitoring.entity.Incident;
import com.omar.incident_monitoring.entity.Incident.IncidentSeverity;
import com.omar.incident_monitoring.entity.Incident.IncidentStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    List<Incident> findAllByApplicationId(Long applicationId);

    List<Incident> findAllByServiceId(Long serviceId);

    Optional<Incident> findByIdAndApplicationId(Long id, Long applicationId);

    @Query("""
            SELECT i FROM Incident i
            WHERE i.application.id = :applicationId
             AND (:status IS NULL OR i.status = :status)
             AND (:severity IS NULL OR i.severity = :severity)
             AND (CAST(:startedAfter AS timestamp) IS NULL
                 OR i.startedAt >= :startedAfter)
             AND (CAST(:startedBefore AS timestamp) IS NULL
                 OR i.startedAt <= :startedBefore)
                    """)
                    
    Page<Incident> findByApplicationIdAndOptionalFilters(
            @Param("applicationId") Long applicationId,
            @Param("status") IncidentStatus status,
            @Param("severity") IncidentSeverity severity,
            @Param("startedAfter") LocalDateTime startedAfter,
            @Param("startedBefore") LocalDateTime startedBefore,
            Pageable pageable);

}
