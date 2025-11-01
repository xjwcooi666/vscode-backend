package com.pigsty.backend.controller;

import com.pigsty.backend.model.Pigsty;
import com.pigsty.backend.repository.PigstyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pigsties")
public class PigstyController {

    @Autowired
    private PigstyRepository pigstyRepository;

    /**
     * API 1: 获取所有猪舍列表
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Pigsty>> getAllPigsties() {
        List<Pigsty> pigsties = pigstyRepository.findAll();
        return ResponseEntity.ok(pigsties);
    }

    /**
     * API 2: 创建一个新的猪舍
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Pigsty> createPigsty(@RequestBody Pigsty pigsty) {
        // [!!! 关键改动 !!!] 
        // 确保所有字段都被保存
        // @RequestBody 会自动把 JSON 映射到 Pigsty 对象的所有字段
        Pigsty savedPigsty = pigstyRepository.save(pigsty);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedPigsty);
    }

    /**
     * API 3: 更新一个猪舍的信息 (按 ID)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Pigsty> updatePigsty(@PathVariable Long id, @RequestBody Pigsty pigstyDetails) {
        
        return pigstyRepository.findById(id)
            .map(existingPigsty -> {
                // 更新基本信息
                existingPigsty.setName(pigstyDetails.getName());
                existingPigsty.setLocation(pigstyDetails.getLocation());
                existingPigsty.setCapacity(pigstyDetails.getCapacity());
                existingPigsty.setTechnicianId(pigstyDetails.getTechnicianId());
                
                // 更新所有阈值
                existingPigsty.setTempThresholdHigh(pigstyDetails.getTempThresholdHigh());
                existingPigsty.setTempThresholdLow(pigstyDetails.getTempThresholdLow());
                existingPigsty.setHumidityThresholdHigh(pigstyDetails.getHumidityThresholdHigh());
                existingPigsty.setHumidityThresholdLow(pigstyDetails.getHumidityThresholdLow());
                existingPigsty.setAmmoniaThresholdHigh(pigstyDetails.getAmmoniaThresholdHigh());
                
                // [!!! 关键修复 !!!] 添加光照阈值的保存逻辑
                existingPigsty.setLightThresholdHigh(pigstyDetails.getLightThresholdHigh());
                existingPigsty.setLightThresholdLow(pigstyDetails.getLightThresholdLow());
                
                Pigsty updatedPigsty = pigstyRepository.save(existingPigsty);
                return ResponseEntity.ok(updatedPigsty);
            })
            .orElse(ResponseEntity.notFound().build()); // 如果找不到，返回 404
    }

    /**
     * API 4: 删除一个猪舍 (按 ID)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePigsty(@PathVariable Long id) {
        if (pigstyRepository.existsById(id)) {
            pigstyRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}