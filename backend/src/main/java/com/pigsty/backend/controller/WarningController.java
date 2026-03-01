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
     * GET /api/warnings/latest?acknowledged=false/true
     * 支持查询未处理/已处理预警（分页、按角色过滤）
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

            Page<WarningLog> filteredPage = logRepository.searchForTechnician(acknowledged, userPigstyIds, metricFilter, pageable);
            return ResponseEntity.ok(filteredPage);
        }
    }

    /**
     * POST /api/warnings/acknowledge/{id}
     * 标记预警为已处理
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
