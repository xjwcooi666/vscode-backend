package com.pigsty.backend.service;

import com.pigsty.backend.model.EnvironmentalData;
import com.pigsty.backend.model.WarningLog;
import com.pigsty.backend.model.WarningRule;
import com.pigsty.backend.repository.WarningLogRepository;
import com.pigsty.backend.repository.WarningRuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service // 告诉 Spring 这是一个服务类
public class WarningService {

    @Autowired // 自动注入
    private WarningRuleRepository ruleRepository;

    @Autowired // 自动注入
    private WarningLogRepository logRepository;

    /**
     * 核心方法：检查新数据是否触发任何预警
     * @param data 刚上报的环境数据
     */
    public void checkDataForWarnings(EnvironmentalData data) {
        // 1. 获取所有当前“启用”的规则
        List<WarningRule> activeRules = ruleRepository.findByEnabledTrue();

        // 2. 遍历每一条规则，看是否匹配
        for (WarningRule rule : activeRules) {

            // 检查规则是否适用于这个猪舍 (匹配 ID 或者规则是 "ALL")
            boolean pigstyMatch = rule.getPigstyId().equalsIgnoreCase("ALL") || 
                                  rule.getPigstyId().equalsIgnoreCase(data.getPigstyId());

            if (pigstyMatch && evaluateRule(data, rule)) {
                // 3. 如果规则被触发，就创建一条预警日志
                createWarningLog(data, rule);
            }
        }
    }

    /**
     * 私有辅助方法：评估单条规则
     * @return true 如果规则被触发, false 如果没有
     */
    private boolean evaluateRule(EnvironmentalData data, WarningRule rule) {
        Double actualValue = null;
        Double threshold = rule.getThreshold();
        String metricType = rule.getMetricType();
        String operator = rule.getOperator();

        // 根据规则的 metricType，获取环境数据中对应的值
        if ("temperature".equalsIgnoreCase(metricType)) {
            actualValue = data.getTemperature();
        } else if ("humidity".equalsIgnoreCase(metricType)) {
            actualValue = data.getHumidity();
        } else if ("ammoniaLevel".equalsIgnoreCase(metricType)) {
            actualValue = data.getAmmoniaLevel();
        }

        // 如果数据有效 (不是 null)
        if (actualValue != null) {
            // 根据操作符进行比较
            switch (operator) {
                case ">":
                    return actualValue > threshold;
                case "<":
                    return actualValue < threshold;
                case "=":
                    return actualValue.equals(threshold);
                // 你以后可以扩展更多操作符, 比如 ">="
            }
        }
        return false; // 无法评估或未触发
    }

    /**
     * 私有辅助方法：创建并保存预警日志
     */
    private void createWarningLog(EnvironmentalData data, WarningRule rule) {
        WarningLog log = new WarningLog();
        log.setPigstyId(data.getPigstyId());
        log.setMessage(rule.getMessage()); // "温度过高！"
        log.setMetricType(rule.getMetricType()); // "temperature"
        log.setThresholdValue(rule.getThreshold()); // 30.0

        // 记录实际超标的值
        if ("temperature".equalsIgnoreCase(rule.getMetricType())) {
            log.setActualValue(data.getTemperature());
        } else if ("humidity".equalsIgnoreCase(rule.getMetricType())) {
            log.setActualValue(data.getHumidity());
        } else if ("ammoniaLevel".equalsIgnoreCase(rule.getMetricType())) {
            log.setActualValue(data.getAmmoniaLevel());
        }

        // 保存到数据库
        logRepository.save(log);

        // (可选) 可以在这里添加发送邮件或短信的逻辑
        System.out.println("!!! 预警触发 !!!: " + log.getMessage() + " 猪舍: " + log.getPigstyId());
    }
}
