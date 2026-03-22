package com.pigsty.backend.service;

import com.pigsty.backend.model.Device;
import com.pigsty.backend.model.EnvironmentalData;
import com.pigsty.backend.model.Pigsty;
import com.pigsty.backend.model.WarningLog;
import com.pigsty.backend.repository.DataRepository;
import com.pigsty.backend.repository.DeviceRepository;
import com.pigsty.backend.repository.PigstyRepository;
import com.pigsty.backend.repository.WarningRepository;
import com.pigsty.backend.controller.WebSocketHandler;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class DataSimulationService {

    private static final Logger log = LoggerFactory.getLogger(DataSimulationService.class);
    private static final double FAULT_INJECTION_PROBABILITY = 0.99;

    private final DataRepository dataRepository;
    private final PigstyRepository pigstyRepository;
    private final DeviceRepository deviceRepository;
    private final WarningRepository warningRepository;
    private final WarningService warningService;
    private final Random random = new Random();

    @Value("${pigsty.simulator.enabled:true}")
    private boolean simulatorEnabled;

    public DataSimulationService(
            DataRepository dataRepository,
            PigstyRepository pigstyRepository,
            DeviceRepository deviceRepository,
            WarningRepository warningRepository,
            WarningService warningService) {
        this.dataRepository = dataRepository;
        this.pigstyRepository = pigstyRepository;
        this.deviceRepository = deviceRepository;
        this.warningRepository = warningRepository;
        this.warningService = warningService;
    }

    @Scheduled(fixedRateString = "${pigsty.simulator.interval-ms:300000}")
    public void pushSimulatedBatch() {
        if (!simulatorEnabled) {
            return;
        }

        List<Pigsty> pigsties = pigstyRepository.findAll();
        if (pigsties.isEmpty()) {
            log.debug("No pigsty found; skip simulation.");
            return;
        }

        injectRandomFault();

        pigsties.forEach(this::generateForPigsty);
    }

    private void injectRandomFault() {
        if (random.nextDouble() >= FAULT_INJECTION_PROBABILITY) {
            return;
        }

        List<Device> onlineDevices = deviceRepository.findAll().stream()
                .filter(Device::isOnline)
                .filter(d -> d.isActive())
                .collect(Collectors.toList());

        if (onlineDevices.isEmpty()) {
            return;
        }

        Device targetDevice = onlineDevices.get(random.nextInt(onlineDevices.size()));
        targetDevice.setOperatingStatus("error");
        deviceRepository.save(targetDevice);

        log.warn("Chaos Testing: Device {} (Pigsty {}) status changed to error", 
                targetDevice.getId(), targetDevice.getPigstyId());

        createDeviceFaultWarning(targetDevice);
    }

    private String getDeviceTypeName(Device.MetricType type) {
        switch (type) {
            case TEMPERATURE: return "温度传感器";
            case HUMIDITY: return "湿度传感器";
            case AMMONIA: return "氨气传感器";
            case LIGHT: return "光照传感器";
            default: return "传感器";
        }
    }

    private void createDeviceFaultWarning(Device device) {
        WarningLog warning = new WarningLog();
        warning.setPigstyId(String.valueOf(device.getPigstyId()));
        warning.setMetricType("设备故障");
        String deviceName = getDeviceTypeName(device.getType());
        warning.setMessage(String.format("%s发生硬件故障，数据已阻断，请立即派人检修！", deviceName));
        warning.setAcknowledged(false);
        warning.setTimestamp(LocalDateTime.now());
        warning.setLevel("DANGER");
        warning.setActualValue(null);
        warning.setThresholdValue(null);
        warningRepository.save(warning);
        
        log.warn("Device fault warning created: Device {} in Pigsty {} - {}", 
                device.getId(), device.getPigstyId(), warning.getMessage());
    }

    private void generateForPigsty(Pigsty pigsty) {
        List<Device> devices = deviceRepository.findByPigstyId(pigsty.getId());
        
        if (devices.isEmpty()) {
            EnvironmentalData data = createEnvironmentalData(pigsty);
            EnvironmentalData saved = dataRepository.save(data);
            WebSocketHandler.sendDataUpdate(saved);
            warningService.checkDataForWarnings(saved);
            return;
        }

        boolean hasOnlineDevice = devices.stream()
                .anyMatch(d -> d.isOnline() && d.isActive());

        if (!hasOnlineDevice) {
            log.debug("Pigsty {} has no online devices, skipping data generation", pigsty.getId());
            return;
        }

        EnvironmentalData data = createEnvironmentalData(pigsty);
        EnvironmentalData saved = dataRepository.save(data);
        WebSocketHandler.sendDataUpdate(saved);
        warningService.checkDataForWarnings(saved);

        devices.stream()
                .filter(d -> d.isOnline() && d.isActive())
                .forEach(device -> {
                    device.setLastHeartbeat(LocalDateTime.now());
                    deviceRepository.save(device);
                });
    }

    private EnvironmentalData createEnvironmentalData(Pigsty pigsty) {
        EnvironmentalData data = new EnvironmentalData();
        data.setPigstyId(String.valueOf(pigsty.getId()));

        data.setTemperature(generateMetric(
                midpoint(pigsty.getTempThresholdLow(), pigsty.getTempThresholdHigh(), 24.0),
                1.5,
                pigsty.getTempThresholdLow(),
                pigsty.getTempThresholdHigh()));

        data.setHumidity(generateMetric(
                midpoint(pigsty.getHumidityThresholdLow(), pigsty.getHumidityThresholdHigh(), 60.0),
                3.0,
                pigsty.getHumidityThresholdLow(),
                pigsty.getHumidityThresholdHigh()));

        data.setAmmoniaLevel(generateMetric(
                midpoint(null, pigsty.getAmmoniaThresholdHigh(), 15.0),
                2.0,
                null,
                pigsty.getAmmoniaThresholdHigh()));

        data.setLight(generateMetric(
                midpoint(pigsty.getLightThresholdLow(), pigsty.getLightThresholdHigh(), 300.0),
                15.0,
                pigsty.getLightThresholdLow(),
                pigsty.getLightThresholdHigh()));

        return data;
    }

    private double generateMetric(double base, double spread, Double low, Double high) {
        double value = base + random.nextGaussian() * spread;

        double chance = random.nextDouble();
        if (high != null && chance < 0.05) {
            value = high + 1.0 + random.nextDouble() * 3.0;
        } else if (low != null && chance > 0.95) {
            value = Math.max(0.0, low - 1.0 - random.nextDouble() * 3.0);
        }

        return roundToTwo(Math.max(0.0, value));
    }

    private double midpoint(Double low, Double high, double fallback) {
        if (low != null && high != null) {
            return (low + high) / 2.0;
        }
        if (low != null) {
            return low + 2.0;
        }
        if (high != null) {
            return Math.max(0.0, high - 2.0);
        }
        return fallback;
    }

    private double roundToTwo(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
