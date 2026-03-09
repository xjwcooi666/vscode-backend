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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 环境数据控制器
 * 
 * 该控制器负责处理猪舍环境数据的上报和查询请求。
 * 所有接口路径都以 /api/data 为前缀。
 * 
 * 主要功能：
 * - 数据上报：接收物联网设备上报的环境数据并触发告警检查
 * - 数据查询：获取最新环境数据（支持基于角色的数据过滤）
 * - 猪舍数据查询：获取特定猪舍的历史环境数据
 * 
 * @author 系统架构
 * @version 1.0
 */
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
     * 上报环境数据
     * 
     * 接收物联网设备上报的环境监测数据，执行以下操作：
     * 1. 将数据保存到数据库
     * 2. 调用告警服务检查数据是否超出阈值
     * 3. 返回保存后的数据对象
     * 
     * 接口路径: POST /api/data
     * 
     * @param data 环境数据对象，包含猪舍ID、指标类型、数值、时间戳等信息
     * @return 保存成功的环境数据对象
     */
    @PostMapping
    public ResponseEntity<EnvironmentalData> addData(@RequestBody EnvironmentalData data) {
        EnvironmentalData savedData = dataRepository.save(data);
        warningService.checkDataForWarnings(savedData);
        return ResponseEntity.ok(savedData);
    }

    /**
     * 获取最新环境数据
     * 
     * 获取系统中最新的100条环境监测数据，根据用户角色进行数据过滤：
     * - ADMIN 角色：可查看所有猪舍的数据
     * - USER 角色：只能查看分配给自己的猪舍的数据
     * 
     * 执行流程：
     * 1. 从安全上下文获取当前登录用户
     * 2. 查询最新100条环境数据
     * 3. 根据用户角色过滤数据
     * 4. 返回过滤后的数据列表
     * 
     * 接口路径: GET /api/data/latest
     * 
     * @return 环境数据列表，未授权返回 401 状态码
     */
    @GetMapping("/latest")
    public ResponseEntity<List<EnvironmentalData>> getLatestData() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByUsername(authentication.getName()).orElse(null);

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<EnvironmentalData> allData = dataRepository.findTop100ByOrderByTimestampDesc();
        
        if (currentUser.getRole() == Role.ADMIN) {
            return ResponseEntity.ok(allData);
        } else {
            List<Pigsty> userPigsties = pigstyRepository.findByTechnicianId(currentUser.getId());
            List<String> userPigstyIds = userPigsties.stream()
                                            .map(pigsty -> String.valueOf(pigsty.getId()))
                                            .collect(Collectors.toList());
            
            List<EnvironmentalData> filteredData = allData.stream()
                .filter(data -> userPigstyIds.contains(data.getPigstyId()))
                .collect(Collectors.toList());
                
            return ResponseEntity.ok(filteredData);
        }
    }

    /**
     * 获取特定猪舍的环境数据
     * 
     * 查询指定猪舍的所有历史环境数据，按时间戳倒序排列。
     * 
     * 注意：当前接口暂未添加权限检查，建议后续增加权限验证，
     * 确保技术员只能访问分配给自己的猪舍数据。
     * 
     * 接口路径: GET /api/data/pigsty/{pigstyId}
     * 
     * @param pigstyId 猪舍ID（路径参数）
     * @return 该猪舍的环境数据列表，按时间倒序排列
     */
    @GetMapping("/pigsty/{pigstyId}")
    public ResponseEntity<List<EnvironmentalData>> getDataByPigstyId(@PathVariable String pigstyId) {
        List<EnvironmentalData> dataList = dataRepository.findByPigstyIdOrderByTimestampDesc(pigstyId);
        return ResponseEntity.ok(dataList);
    }
}
