package com.pigsty.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

/**
 * 设备实体类
 * 
 * 该类表示系统中的一个监测设备，用于采集猪舍的环境数据。
 * 每个设备属于一个猪舍，可以监测特定类型的环境指标。
 * 
 * @author 系统架构
 * @version 1.0
 */
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
    
    /**
     * 设备监测指标类型枚举
     */
    public enum MetricType {
        TEMPERATURE,
        HUMIDITY,
        AMMONIA,
        LIGHT
    }
}
