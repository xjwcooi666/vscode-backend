package com.pigsty.backend.controller;

import com.pigsty.backend.model.Pigsty;
import com.pigsty.backend.repository.PigstyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // 导入权限注解
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pigsties") // 所有 API 都在 /api/pigsties 路径下
public class PigstyController {

    @Autowired
    private PigstyRepository pigstyRepository;

    /**
     * API 1: 获取所有猪舍列表
     * 任何人登录了都能看 (ADMIN 和 USER)
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()") // 只要登录了就行
    public ResponseEntity<List<Pigsty>> getAllPigsties() {
        List<Pigsty> pigsties = pigstyRepository.findAll();
        return ResponseEntity.ok(pigsties);
    }

    /**
     * API 2: 创建一个新的猪舍
     * 只有 ADMIN 能创建
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')") // 必须是 ADMIN 角色
    public ResponseEntity<Pigsty> createPigsty(@RequestBody Pigsty pigsty) {
        Pigsty savedPigsty = pigstyRepository.save(pigsty);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedPigsty);
    }

    /**
     * API 3: 更新一个猪舍的信息 (按 ID)
     * 只有 ADMIN 能更新
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Pigsty> updatePigsty(@PathVariable Long id, @RequestBody Pigsty pigstyDetails) {
        
        return pigstyRepository.findById(id)
            .map(existingPigsty -> {
                // 更新字段
                existingPigsty.setName(pigstyDetails.getName());
                existingPigsty.setLocation(pigstyDetails.getLocation());
                existingPigsty.setCapacity(pigstyDetails.getCapacity());
                existingPigsty.setTechnicianId(pigstyDetails.getTechnicianId()); // 更新分配的技术员
                
                // 更新所有阈值
                existingPigsty.setTempThresholdHigh(pigstyDetails.getTempThresholdHigh());
                existingPigsty.setTempThresholdLow(pigstyDetails.getTempThresholdLow());
                existingPigsty.setHumidityThresholdHigh(pigstyDetails.getHumidityThresholdHigh());
                existingPigsty.setHumidityThresholdLow(pigstyDetails.getHumidityThresholdLow());
                existingPigsty.setAmmoniaThresholdHigh(pigstyDetails.getAmmoniaThresholdHigh());
                
                Pigsty updatedPigsty = pigstyRepository.save(existingPigsty);
                return ResponseEntity.ok(updatedPigsty);
            })
            .orElse(ResponseEntity.notFound().build()); // 如果找不到，返回 404
    }

    /**
     * API 4: 删除一个猪舍 (按 ID)
     * 只有 ADMIN 能删除
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePigsty(@PathVariable Long id) {
        if (pigstyRepository.existsById(id)) {
            pigstyRepository.deleteById(id);
            return ResponseEntity.noContent().build(); // 返回 204 No Content
        } else {
            return ResponseEntity.notFound().build(); // 返回 404
        }
    }
}
