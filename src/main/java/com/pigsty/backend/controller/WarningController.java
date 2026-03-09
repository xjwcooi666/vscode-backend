package com.pigsty.backend.controller;

import com.pigsty.backend.model.Pigsty;
import com.pigsty.backend.model.Role;
import com.pigsty.backend.model.User;
import com.pigsty.backend.model.WarningLog;
import com.pigsty.backend.repository.PigstyRepository;
import com.pigsty.backend.repository.UserRepository;
import com.pigsty.backend.repository.WarningLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 告警管理控制器
 * 
 * 该控制器负责处理环境告警的查询和确认操作。
 * 所有接口路径都以 /api/warnings 为前缀。
 * 
 * 主要功能：
 * - 获取告警列表：支持分页、按确认状态筛选、按角色过滤数据
 * - 确认告警：将告警标记为已处理状态
 * 
 * @author 系统架构
 * @version 1.0
 */
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
     * 获取最新告警列表
     * 
     * 分页查询系统中的告警记录，支持多种筛选条件。
     * 根据用户角色进行数据过滤：
     * - ADMIN 角色：可查看所有猪舍的告警
     * - USER 角色：只能查看分配给自己的猪舍的告警
     * 
     * 接口路径: GET /api/warnings/latest
     * 
     * @param page 页码，从0开始，默认为0
     * @param size 每页条数，默认为100
     * @param acknowledged 是否只显示已确认的告警，默认为false
     * @param pigstyId 猪舍ID筛选，可选，空值或"all"表示不筛选
     * @param metricType 指标类型筛选，可选，空值或"all"表示不筛选
     * @return 分页告警数据，未授权返回401
     */
    @GetMapping("/latest")
    public ResponseEntity<Page<WarningLog>> getLatestWarnings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(defaultValue = "false") boolean acknowledged,
            @RequestParam(required = false) String pigstyId,
            @RequestParam(required = false) String metricType) {

        // 预处理筛选参数：空串/“all”视为未筛选
        String pigstyFilter = (pigstyId == null || pigstyId.isBlank() || "all".equalsIgnoreCase(pigstyId)) ? null : pigstyId;
        String metricFilter = (metricType == null || metricType.isBlank() || "all".equalsIgnoreCase(metricType)) ? null : metricType;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Sort sort = acknowledged
                ? Sort.by(Sort.Direction.DESC, "acknowledgedAt", "timestamp") // 已处理按处理时间，空值回退发生时间
                : Sort.by(Sort.Direction.DESC, "timestamp");
        Pageable pageable = PageRequest.of(page, size, sort);

        if (currentUser.getRole() == Role.ADMIN) {
            Page<WarningLog> pageResult = logRepository.searchForAdmin(acknowledged, pigstyFilter, metricFilter, pageable);
            return ResponseEntity.ok(pageResult);
        } else {
            List<Pigsty> userPigsties = pigstyRepository.findByTechnicianId(currentUser.getId());
            List<String> userPigstyIds = userPigsties.stream()
                    .map(pigsty -> String.valueOf(pigsty.getId()))
                    .collect(Collectors.toList());

            if (userPigstyIds.isEmpty()) {
                Page<WarningLog> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
                return ResponseEntity.ok(emptyPage);
            }

            List<String> filteredPigstyIds;
            if (pigstyFilter != null && !pigstyFilter.equals("all")) {
                filteredPigstyIds = userPigstyIds.stream()
                        .filter(id -> id.equals(pigstyFilter))
                        .collect(Collectors.toList());
            } else {
                filteredPigstyIds = userPigstyIds;
            }

            if (filteredPigstyIds.isEmpty()) {
                Page<WarningLog> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
                return ResponseEntity.ok(emptyPage);
            }

            Page<WarningLog> filteredPage = logRepository.searchForTechnician(acknowledged, filteredPigstyIds, metricFilter, pageable);
            return ResponseEntity.ok(filteredPage);
        }
    }

    /**
     * 确认告警
     * 
     * 将指定ID的告警标记为已处理状态，并记录确认时间。
     * 
     * 接口路径: POST /api/warnings/acknowledge/{id}
     * 
     * @param id 告警记录的ID
     * @return 更新后的告警对象，找不到返回404
     */
    @PostMapping("/acknowledge/{id}")
    public ResponseEntity<WarningLog> acknowledgeWarning(@PathVariable Long id) {
        Optional<WarningLog> logOpt = logRepository.findById(id);
        if (logOpt.isPresent()) {
            WarningLog log = logOpt.get();
            log.setAcknowledged(true);
            log.setAcknowledgedAt(java.time.LocalDateTime.now());
            WarningLog updatedLog = logRepository.save(log);
            return ResponseEntity.ok(updatedLog);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
