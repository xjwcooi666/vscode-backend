package com.pigsty.backend.service;

import com.pigsty.backend.model.EnvironmentalData;
import com.pigsty.backend.model.Pigsty;
import com.pigsty.backend.repository.DataRepository;
import com.pigsty.backend.repository.PigstyRepository;
import com.pigsty.backend.controller.WebSocketHandler;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 数据模拟服务
 * 
 * 该服务定期为每个猪舍生成模拟环境数据，便于前端持续刷新展示和测试系统功能。
 * 
 * 主要功能：
 * - 按配置的时间间隔自动生成模拟数据
 * - 基于猪舍阈值范围生成正态分布的合理数据
 * - 偶发生成异常数据以触发告警测试
 * - 生成数据后自动触发告警检查
 * 
 * 配置项：
 * - pigsty.simulator.enabled: 是否启用模拟器（默认：true）
 * - pigsty.simulator.interval-ms: 数据生成间隔（默认：300000ms，即5分钟）
 * 
 * @author 系统架构
 * @version 1.0
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

    /**
     * 构造函数
     * 
     * @param dataRepository 环境数据仓库
     * @param pigstyRepository 猪舍仓库
     * @param warningService 告警服务
     */
    public DataSimulationService(
            DataRepository dataRepository,
            PigstyRepository pigstyRepository,
            WarningService warningService) {
        this.dataRepository = dataRepository;
        this.pigstyRepository = pigstyRepository;
        this.warningService = warningService;
    }

    /**
     * 按固定频率为所有猪舍推送一批新数据
     * 
     * 定时任务方法，按配置的时间间隔自动执行。
     * 如果模拟器未启用或没有猪舍数据，则直接返回。
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

    /**
     * 为单个猪舍生成模拟环境数据
     * 
     * 该方法根据猪舍的阈值配置生成温度、湿度、氨气浓度和光照的模拟数据，
     * 然后保存到数据库并触发告警检查。
     * 
     * @param pigsty 目标猪舍对象
     */
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
        // 通过 WebSocket 推送数据更新消息
        WebSocketHandler.sendDataUpdate(saved);
        warningService.checkDataForWarnings(saved);
    }

    /**
     * 生成单个指标的模拟数值
     * 
     * 该方法基于基准值和标准差生成正态分布的数值。
     * 偶发（各5%概率）生成超出阈值范围的异常值以触发告警测试。
     * 
     * @param base 基准值（阈值中点或默认值）
     * @param spread 标准差，控制数值波动范围
     * @param low 下限阈值，可为null
     * @param high 上限阈值，可为null
     * @return 生成的模拟数值，保留两位小数
     */
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

    /**
     * 计算阈值范围的中点
     * 
     * 根据上下限阈值计算合适的基准值。
     * 如果上下限都存在，返回中点；
     * 如果只有下限，返回下限+2；
     * 如果只有上限，返回上限-2；
     * 如果都没有，返回默认值。
     * 
     * @param low 下限阈值，可为null
     * @param high 上限阈值，可为null
     * @param fallback 默认值
     * @return 计算得到的基准值
     */
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

    /**
     * 将数值四舍五入到两位小数
     * 
     * @param value 原始数值
     * @return 保留两位小数的数值
     */
    private double roundToTwo(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
