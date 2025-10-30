package com.pigsty.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class WarningRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String pigstyId;    // 规则应用于哪个猪舍 (例如 "A-01", 或者 "ALL")
    private String metricType;  // 监控的指标 (例如 "temperature", "humidity", "ammoniaLevel")
    private String operator;    // 操作符 (例如 ">", "<", "=")
    private Double threshold;   // 阈值 (例如 30.0)
    private String message;     // 预警信息 (例如 "温度过高！")
    private boolean enabled = true; // 规则是否启用
}
