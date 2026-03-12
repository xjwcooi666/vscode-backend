package com.pigsty.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 环境数据实体类
 * 
 * 该类表示从猪舍设备采集的一次环境数据记录，包含温度、湿度、氨气浓度和光照等指标。
 * 每次数据上报时会自动设置时间戳。
 * 
 * @author 系统架构
 * @version 1.0
 */
@Entity
@Data
@Table(name = "environmental_data", indexes = {
    @Index(name = "idx_pigsty_id", columnList = "pigsty_id"),
    @Index(name = "idx_timestamp", columnList = "timestamp")
})
public class EnvironmentalData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double temperature;
    private Double humidity;
    private Double ammoniaLevel;

    private Double light;

    private String pigstyId;

    private LocalDateTime timestamp;

    @jakarta.persistence.PrePersist
    public void prePersist() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }
}