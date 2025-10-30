package com.pigsty.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class WarningLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String pigstyId;          // 哪个猪舍出的问题
    private String message;           // 预警信息 (来自 WarningRule)
    private String metricType;        // 哪个指标出的问题
    private Double actualValue;       // 实际的超标数值
    private Double thresholdValue;    // 当时的阈值
    private LocalDateTime timestamp;  // 预警发生时间

    private boolean acknowledged = false; // 预警是否已被“确认处理”

    @jakarta.persistence.PrePersist
    public void prePersist() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }
}
