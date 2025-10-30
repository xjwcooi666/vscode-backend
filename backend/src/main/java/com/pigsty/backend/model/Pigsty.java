package com.pigsty.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data // 自动生成 Getter, Setter, etc.
public class Pigsty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;       // 例如 "A-01号猪舍" 或 "保育舍 #1"
    private String location;   // 例如 "1号楼东区"
    private Integer capacity;  // 猪舍容量

    // 猪舍分配给哪个技术员 (我们只存储技术员的 User ID)
    private Long technicianId; 

    // --- 阈值 ---
    // 这些是和 `WarningRule` 分开的，用于显示在 UI 上的“常规设置”
    // (我们也可以在未来让 `WarningService` 来读取这些值)
    private Double tempThresholdHigh;     // 温度上限
    private Double tempThresholdLow;      // 温度下限
    private Double humidityThresholdHigh; // 湿度上限
    private Double humidityThresholdLow;  // 湿度下限
    private Double ammoniaThresholdHigh;  // 氨气上限
}
