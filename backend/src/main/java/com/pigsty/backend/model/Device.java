package com.pigsty.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Entity
@Data
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long pigstyId; // 这个设备属于哪个猪舍 (Pigsty ID)

    @Enumerated(EnumType.STRING) // 按字符串存储设备类型
    private MetricType type;   // 设备类型 (例如 TEMPERATURE, HUMIDITY)

    private String modelNumber; // 设备型号 (可选)
    private String serialNumber; // 设备序列号 (可选, 应该唯一)

    private boolean isActive = true; // 设备是否启用/在线
    
    // 我们需要 MetricType 枚举
    // 如果你的 types.ts 里有 MetricType，确保后端也有一个类似的
    // 我们可以先在这里定义一个简单的
    public enum MetricType {
        TEMPERATURE,
        HUMIDITY,
        AMMONIA,
        LIGHT 
        // 你可以根据前端的 types.ts 添加更多类型
    }
}

// 注意：如果你已经在其他地方定义了 MetricType 枚举 (例如在 Model 包下创建 MetricType.java)
// 请删除上面内部定义的 MetricType，并确保这里正确导入了外部的 MetricType。
