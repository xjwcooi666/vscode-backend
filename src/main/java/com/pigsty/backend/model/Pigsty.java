package com.pigsty.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

/**
 * 猪舍实体类
 * 
 * 该类表示系统中的一个猪舍，包含基本信息和各项环境指标的阈值配置。
 * 阈值用于判断环境数据是否正常，超出范围时会触发告警。
 * 
 * @author 系统架构
 * @version 1.0
 */
@Entity
@Data
public class Pigsty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String location;
    private Integer capacity;
    private Long technicianId; 

    private Double tempThresholdHigh;
    private Double tempThresholdLow;
    
    private Double humidityThresholdHigh;
    private Double humidityThresholdLow;
    
    private Double ammoniaThresholdHigh;

    private Double lightThresholdHigh;
    private Double lightThresholdLow;
}
