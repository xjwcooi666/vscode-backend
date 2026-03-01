package com.pigsty.backend.service;

import com.pigsty.backend.model.EnvironmentalData;
import com.pigsty.backend.model.Pigsty;
import com.pigsty.backend.repository.DataRepository;
import com.pigsty.backend.repository.PigstyRepository;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 定期为每个猪舍生成模拟数据，便于前端持续刷新展示。
 */
@Service
public class DataSimulationService {

    private static final Logger log = LoggerFactory.getLogger(DataSimulationService.class);

    private final DataRepository dataRepository;
    private final PigstyRepository pigstyRepository;
    private final WarningService warningService;
    private final Random random = new Random();

    @Value("${pigsty.simulator.enabled:true}")
    private boolean simulatorEnabled;

    public DataSimulationService(
            DataRepository dataRepository,
            PigstyRepository pigstyRepository,
            WarningService warningService) {
        this.dataRepository = dataRepository;
        this.pigstyRepository = pigstyRepository;
        this.warningService = warningService;
    }

    /**
     * 按固定频率为所有猪舍推送一批新数据。
     */
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

        pigsties.forEach(this::generateForPigsty);
    }

    private void generateForPigsty(Pigsty pigsty) {
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

        EnvironmentalData saved = dataRepository.save(data);
        warningService.checkDataForWarnings(saved);
    }

    private double generateMetric(double base, double spread, Double low, Double high) {
        double value = base + random.nextGaussian() * spread;

        // 偶发“异常”便于触发预警，概率较低
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
