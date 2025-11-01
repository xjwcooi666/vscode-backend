package com.pigsty.backend.controller;

import com.pigsty.backend.model.EnvironmentalData;
import com.pigsty.backend.model.Pigsty;
import com.pigsty.backend.model.Role;
import com.pigsty.backend.model.User;
import com.pigsty.backend.repository.DataRepository;
import com.pigsty.backend.repository.PigstyRepository;
import com.pigsty.backend.repository.UserRepository;
import com.pigsty.backend.service.WarningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus; // [!!! 关键修复 !!!] 添加这个导入
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/data")
public class DataController {

    @Autowired
    private DataRepository dataRepository;

    @Autowired
    private WarningService warningService;

    @Autowired
    private PigstyRepository pigstyRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 接口 1：上报数据 (保持不变)
     */
    @PostMapping
    public ResponseEntity<EnvironmentalData> addData(@RequestBody EnvironmentalData data) {
        EnvironmentalData savedData = dataRepository.save(data);
        warningService.checkDataForWarnings(savedData);
        return ResponseEntity.ok(savedData);
    }

    /**
     * 接口 2：获取最新的环境数据 (已修复权限)
     * GET /api/data/latest
     */
    @GetMapping("/latest")
    public ResponseEntity<List<EnvironmentalData>> getLatestData() {
        // 1. 获取当前登录的用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByUsername(authentication.getName()).orElse(null);

        if (currentUser == null) {
            // [!!! 关键修复 !!!] HttpStatus 现在可以被识别了
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<EnvironmentalData> allData = dataRepository.findTop100ByOrderByTimestampDesc();
        
        // 2. 检查角色
        if (currentUser.getRole() == Role.ADMIN) {
            // 如果是 Admin，返回所有数据
            return ResponseEntity.ok(allData);
        } else {
            // 如果是 User (技术员)，只返回分配给他的猪舍的数据
            
            // 2a. 找到该技术员的猪舍
            List<Pigsty> userPigsties = pigstyRepository.findByTechnicianId(currentUser.getId());
            // 2b. 提取猪舍 ID (String)
            List<String> userPigstyIds = userPigsties.stream()
                                            .map(pigsty -> String.valueOf(pigsty.getId()))
                                            .collect(Collectors.toList());
            
            // 2c. 过滤 allData
            List<EnvironmentalData> filteredData = allData.stream()
                .filter(data -> userPigstyIds.contains(data.getPigstyId()))
                .collect(Collectors.toList());
                
            return ResponseEntity.ok(filteredData);
        }
    }

    /**
     * 接口 3：获取特定猪舍的数据 (保持不变)
     */
    @GetMapping("/pigsty/{pigstyId}")
    public ResponseEntity<List<EnvironmentalData>> getDataByPigstyId(@PathVariable String pigstyId) {
        // TODO: 这里也应该添加权限检查，确保技术员只能访问他自己的 pigstyId
        List<EnvironmentalData> dataList = dataRepository.findByPigstyIdOrderByTimestampDesc(pigstyId);
        return ResponseEntity.ok(dataList);
    }
}