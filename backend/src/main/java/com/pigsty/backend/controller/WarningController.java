package com.pigsty.backend.controller;

import com.pigsty.backend.model.WarningLog;
import com.pigsty.backend.repository.WarningLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/warnings") // 所有预警日志相关的 API 都在 /api/warnings 下
public class WarningController {

    @Autowired
    private WarningLogRepository logRepository;

    /**
     * API 1: 获取所有“未处理”的最新预警 (给前端仪表盘用)
     * GET /api/warnings/latest
     */
    @GetMapping("/latest")
    public ResponseEntity<List<WarningLog>> getLatestUnacknowledgedWarnings() {
        List<WarningLog> logs = logRepository.findTop100ByAcknowledgedFalseOrderByTimestampDesc();
        return ResponseEntity.ok(logs);
    }

    /**
     * API 2: "确认处理" 一条预警 (前端点击“我知道了”按钮时调用)
     * POST /api/warnings/acknowledge/1
     */
    @PostMapping("/acknowledge/{id}")
    public ResponseEntity<WarningLog> acknowledgeWarning(@PathVariable Long id) {
        Optional<WarningLog> logOpt = logRepository.findById(id);
        if (logOpt.isPresent()) {
            WarningLog log = logOpt.get();
            log.setAcknowledged(true); // 设为“已处理”
            WarningLog updatedLog = logRepository.save(log);
            return ResponseEntity.ok(updatedLog);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
