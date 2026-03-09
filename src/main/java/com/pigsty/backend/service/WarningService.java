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

import java.util.List;
import java.util.Optional;

/**
 * 告警服务
 * 
 * 该服务负责检查环境数据是否超出猪舍设定的阈值，并在超出时创建告警记录。
 * 
 * 主要功能：
 * - 检查环境数据是否触发告警
 * - 验证对应类型的设备是否激活
 * - 创建并保存告警日志
 * 
 * @author 系统架构
 * @version 1.0
 */
@Service
public class WarningService {

    @Autowired
    private PigstyRepository pigstyRepository;

    @Autowired
    private WarningLogRepository logRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    /**
     * 检查环境数据是否触发告警
     * 
     * 核心方法：检查新上报的环境数据是否超出其所属猪舍的各项阈值，
     * 如果超出且对应设备处于激活状态，则创建告警记录。
     * 
     * 检查的指标包括：
     * - 温度（过高/过低）
     * - 湿度（过高/过低）
     * - 氨气浓度（过高）
     * - 光照（过强/过弱）
     * 
     * @param data 刚上报的环境数据对象
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
     * 检查特定类型的设备是否存在且已激活
     * 
     * 该方法从指定设备列表中查找匹配类型的设备，并检查其激活状态。
     * 只有当设备存在且处于激活状态时，才会返回 true。
     * 
     * @param devices 猪舍的设备列表
     * @param type 要检查的设备类型
     * @return 如果设备存在且已激活返回 true，否则返回 false
     */
    private boolean isDeviceActive(List<Device> devices, MetricType type) {
        Optional<Device> deviceOpt = devices.stream()
            .filter(d -> d.getType() == type)
            .findFirst();
        
        if (deviceOpt.isPresent()) {
            return deviceOpt.get().isActive();
        }
        
        return false;
    }

    /**
     * 创建并保存告警日志
     * 
     * 该方法根据指定参数创建新的告警日志对象，并将其保存到数据库中。
     * 同时在控制台打印告警信息便于调试和监控。
     * 
     * @param pigsty 触发告警的猪舍对象
     * @param metricType 触发告警的指标类型
     * @param actualValue 实际测量值
     * @param message 告警详细描述
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

