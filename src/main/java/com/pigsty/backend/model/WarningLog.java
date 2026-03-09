package com.pigsty.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 告警日志实体类
 * 
 * 该类表示系统中触发的一条告警记录，包含告警的详细信息、确认状态和时间戳。
 * 当环境数据超出猪舍设定的阈值时，系统会自动创建一条告警日志。
 * 
 * @author 系统架构
 * @version 1.0
 */
@Entity
@Data
public class WarningLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String pigstyId;
    private String message;
    private String metricType;
    private Double actualValue;
    private Double thresholdValue;
    private LocalDateTime timestamp;

    private boolean acknowledged = false;
    private LocalDateTime acknowledgedAt;

    @jakarta.persistence.PrePersist
    public void prePersist() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }
}
