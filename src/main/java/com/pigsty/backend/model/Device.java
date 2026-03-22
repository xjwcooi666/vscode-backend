package com.pigsty.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Data
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long pigstyId;

    @Enumerated(EnumType.STRING)
    private MetricType type;

    private String modelNumber;
    private String serialNumber;

    private boolean isActive = true;
    
    private String operatingStatus = "online";
    
    private LocalDateTime lastHeartbeat;
    
    public enum MetricType {
        TEMPERATURE,
        HUMIDITY,
        AMMONIA,
        LIGHT
    }
    
    public boolean isOnline() {
        return "online".equals(operatingStatus);
    }
    
    public boolean hasError() {
        return "error".equals(operatingStatus);
    }
    
    public boolean isOffline() {
        return "offline".equals(operatingStatus);
    }
}
