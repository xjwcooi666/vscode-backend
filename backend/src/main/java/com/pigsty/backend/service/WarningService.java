package com.pigsty.backend.service;

import com.pigsty.backend.model.Device; // 1. 导入 Device
import com.pigsty.backend.model.EnvironmentalData;
import com.pigsty.backend.model.Pigsty;
import com.pigsty.backend.model.WarningLog;
import com.pigsty.backend.model.Device.MetricType; // 2. 导入 MetricType
import com.pigsty.backend.repository.DeviceRepository; // 3. 导入 DeviceRepository
import com.pigsty.backend.repository.PigstyRepository;
import com.pigsty.backend.repository.WarningLogRepository; 

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List; // 4. 导入 List
import java.util.Optional;

@Service
public class WarningService {

    @Autowired
    private PigstyRepository pigstyRepository;

    @Autowired
    private WarningLogRepository logRepository;

    @Autowired // 5. 注入 DeviceRepository
    private DeviceRepository deviceRepository;

    /**
     * 核心方法：(已重写)
     * 检查新数据是否触发其 *所属猪舍* 的阈值
     * @param data 刚上报的环境数据
     */
    public void checkDataForWarnings(EnvironmentalData data) {
        
        Long pigstyId;
        try {
            pigstyId = Long.parseLong(data.getPigstyId());
        } catch (NumberFormatException e) {
            System.err.println("Invalid pigstyId format in data: " + data.getPigstyId());
            return;
        }

        Optional<Pigsty> pigstyOpt = pigstyRepository.findById(pigstyId);

        if (!pigstyOpt.isPresent()) {
            System.err.println("Pigsty with ID " + pigstyId + " not found. Cannot check warnings.");
            return;
        }

        Pigsty pigsty = pigstyOpt.get();

        // [!!! 关键修复 !!!]
        // 6. 获取这个猪舍的所有设备
        List<Device> devices = deviceRepository.findByPigstyId(pigstyId);

        // 7. 逐一检查每个指标
        
        // 检查温度
        if (data.getTemperature() != null && isDeviceActive(devices, MetricType.TEMPERATURE)) {
            if (pigsty.getTempThresholdHigh() != null && data.getTemperature() > pigsty.getTempThresholdHigh()) {
                createWarningLog(pigsty, "TEMPERATURE", data.getTemperature(), "温度过高！");
            }
            if (pigsty.getTempThresholdLow() != null && data.getTemperature() < pigsty.getTempThresholdLow()) {
                createWarningLog(pigsty, "TEMPERATURE", data.getTemperature(), "温度过低！");
            }
        }

        // 检查湿度
        if (data.getHumidity() != null && isDeviceActive(devices, MetricType.HUMIDITY)) {
            if (pigsty.getHumidityThresholdHigh() != null && data.getHumidity() > pigsty.getHumidityThresholdHigh()) {
                createWarningLog(pigsty, "HUMIDITY", data.getHumidity(), "湿度过高！");
            }
            if (pigsty.getHumidityThresholdLow() != null && data.getHumidity() < pigsty.getHumidityThresholdLow()) {
                createWarningLog(pigsty, "HUMIDITY", data.getHumidity(), "湿度过低！");
            }
        }
        
        // 检查氨气
        if (data.getAmmoniaLevel() != null && isDeviceActive(devices, MetricType.AMMONIA)) {
            if (pigsty.getAmmoniaThresholdHigh() != null && data.getAmmoniaLevel() > pigsty.getAmmoniaThresholdHigh()) {
                createWarningLog(pigsty, "AMMONIA", data.getAmmoniaLevel(), "氨气浓度过高！");
            }
        }

        // 检查光照
        if (data.getLight() != null && isDeviceActive(devices, MetricType.LIGHT)) {
            if (pigsty.getLightThresholdHigh() != null && data.getLight() > pigsty.getLightThresholdHigh()) {
                createWarningLog(pigsty, "LIGHT", data.getLight(), "光照过强！");
            }
            if (pigsty.getLightThresholdLow() != null && data.getLight() < pigsty.getLightThresholdLow()) {
                createWarningLog(pigsty, "LIGHT", data.getLight(), "光照过弱！");
            }
        }
    }

    /**
     * 8. [!!! 新增辅助方法 !!!]
     * 检查特定类型的设备是否存在且已激活
     */
    private boolean isDeviceActive(List<Device> devices, MetricType type) {
        // 查找匹配该类型的设备
        Optional<Device> deviceOpt = devices.stream()
            .filter(d -> d.getType() == type)
            .findFirst();
        
        if (deviceOpt.isPresent()) {
            // 如果设备存在，检查它是否处于激活状态
            return deviceOpt.get().isActive(); 
        }
        
        // 如果该猪舍 *根本没有* 这种类型的设备，我们也不应该报警
        return false; 
    }

    /**
     * 创建并保存预警日志 (保持不变)
     */
    private void createWarningLog(Pigsty pigsty, String metricType, Double actualValue, String message) {
        WarningLog log = new WarningLog();
        log.setPigstyId(String.valueOf(pigsty.getId()));
        log.setMessage(message);
        log.setMetricType(metricType);
        log.setActualValue(actualValue);

        logRepository.save(log);

        System.out.println("!!! 预警触发 !!!: " + log.getMessage() + " 猪舍: " + log.getPigstyId());
    }
}

