package com.omar.incident_monitoring.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.omar.incident_monitoring.dto.MonitoringBaselineResponse;
import com.omar.incident_monitoring.service.MonitoringService;

@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {

    private final MonitoringService monitoringService;

    public MonitoringController(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @GetMapping("/baseline")
    public MonitoringBaselineResponse getBaseline() {
        return monitoringService.getBaseline();
    }
}
