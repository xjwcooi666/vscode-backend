package com.pigsty.backend.controller;

import com.pigsty.backend.model.Pigsty;
import com.pigsty.backend.model.Role;
import com.pigsty.backend.model.User;
import com.pigsty.backend.model.WarningLog;
import com.pigsty.backend.repository.PigstyRepository;
import com.pigsty.backend.repository.UserRepository;
import com.pigsty.backend.repository.WarningLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus; // [!!! 关键修复 !!!] 添加这个导入
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; 
import org.springframework.security.core.context.SecurityContextHolder; 
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/warnings")
public class WarningController {

    @Autowired
    private WarningLogRepository logRepository;

    @Autowired
    private PigstyRepository pigstyRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * API 1: 获取所有“未处理”的最新预警 (已修复权限)
     * GET /api/warnings/latest
     */
    @GetMapping("/latest")
    public ResponseEntity<List<WarningLog>> getLatestUnacknowledgedWarnings() {
        // 1. 获取当前登录的用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByUsername(authentication.getName()).orElse(null);

        if (currentUser == null) {
            // [!!! 关键修复 !!!] HttpStatus 现在可以被识别了
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); 
        }

        List<WarningLog> allLogs = logRepository.findTop100ByAcknowledgedFalseOrderByTimestampDesc();

        // 2. 检查角色
        if (currentUser.getRole() == Role.ADMIN) {
            // 如果是 Admin，返回所有警报
            return ResponseEntity.ok(allLogs);
        } else {
            // 如果是 User (技术员)，只返回分配给他的猪舍的警报
            
            // 2a. 找到该技术员的猪舍
            List<Pigsty> userPigsties = pigstyRepository.findByTechnicianId(currentUser.getId());
            // 2b. 提取猪舍 ID (String)
            List<String> userPigstyIds = userPigsties.stream()
                                            .map(pigsty -> String.valueOf(pigsty.getId()))
                                            .collect(Collectors.toList());
            
            // 2c. 过滤 allLogs
            List<WarningLog> filteredLogs = allLogs.stream()
                .filter(log -> userPigstyIds.contains(log.getPigstyId())) // [!!! 勘误 !!!] 假设 WarningLog 中是 getPigId() 或 getPigstyId()
                .collect(Collectors.toList());
                
            return ResponseEntity.ok(filteredLogs);
        }
    }

    /**
     * API 2: "确认处理" 一条预警 (保持不变)
     * (安全：技术员也应该能确认他自己猪舍的警报)
     */
    @PostMapping("/acknowledge/{id}")
    public ResponseEntity<WarningLog> acknowledgeWarning(@PathVariable Long id) {
        // TODO: 这里也应该添加权限检查，确保技术员只能确认他自己猪舍的警报
        
        Optional<WarningLog> logOpt = logRepository.findById(id);
        if (logOpt.isPresent()) {
            WarningLog log = logOpt.get();
            log.setAcknowledged(true);
            WarningLog updatedLog = logRepository.save(log);
            return ResponseEntity.ok(updatedLog);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

