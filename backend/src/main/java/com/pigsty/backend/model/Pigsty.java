package com.pigsty.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

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

    // --- 温度 ---
    private Double tempThresholdHigh;
    private Double tempThresholdLow;
    
    // --- 湿度 ---
    private Double humidityThresholdHigh;
    private Double humidityThresholdLow;
    
    // --- 氨气 ---
    private Double ammoniaThresholdHigh;
    // (氨气通常没有下限)

    //  添加光照阈值
    private Double lightThresholdHigh;
    private Double lightThresholdLow;
}
