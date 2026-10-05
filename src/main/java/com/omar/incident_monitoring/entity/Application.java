package com.omar.incident_monitoring.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.List;

@Entity
@Table(name = "applications")
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String name;

    @JdbcTypeCode (SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String,Object> configuration;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy="application")
    private List<Service> services;

    @OneToMany(mappedBy = "application")
    private List<Metric> metrics;

    @OneToMany (mappedBy = "application")
    private List<Experiment> experiments;

    @OneToMany (mappedBy = "application")
    private List<Incident> incidents;

    //setters getters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String,Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String,Object> configuration) {
        this.configuration = configuration;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    

}
