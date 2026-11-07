package com.omar.incident_monitoring.monitoring.prometheus;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class PrometheusUnavailableException extends RuntimeException {

    public PrometheusUnavailableException(String message) {
        super(message);
    }

    public PrometheusUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
