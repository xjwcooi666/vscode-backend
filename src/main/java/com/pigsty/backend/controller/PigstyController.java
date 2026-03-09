package com.pigsty.backend.controller;

import com.pigsty.backend.model.Pigsty;
import com.pigsty.backend.repository.PigstyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 猪舍管理控制器
 * 
 * 该控制器负责处理猪舍的增删改查请求。
 * 所有接口路径都以 /api/pigsties 为前缀。
 * 
 * 主要功能：
 * - 获取猪舍列表：所有已认证用户可访问
 * - 创建猪舍：仅 ADMIN 角色可操作
 * - 更新猪舍：仅 ADMIN 角色可操作
 * - 删除猪舍：仅 ADMIN 角色可操作
 * 
 * @author 系统架构
 * @version 1.0
 */
@RestController
@RequestMapping("/api/pigsties")
public class PigstyController {

    @Autowired
    private PigstyRepository pigstyRepository;

    /**
     * 获取所有猪舍列表
     * 
     * 查询系统中所有猪舍的基本信息。
     * 所有已登录认证的用户都可以访问此接口。
     * 
     * 接口路径: GET /api/pigsties
     * 
     * @return 猪舍列表，包含所有猪舍的完整信息
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Pigsty>> getAllPigsties() {
        List<Pigsty> pigsties = pigstyRepository.findAll();
        return ResponseEntity.ok(pigsties);
    }

    /**
     * 创建新猪舍
     * 
     * 在系统中创建一个新的猪舍记录，包含基本信息和各项环境指标的阈值配置。
     * 仅具有 ADMIN 角色的用户可以执行此操作。
     * 
     * 接口路径: POST /api/pigsties
     * 
     * @param pigsty 猪舍信息对象，包含名称、位置、容量、技术人员ID及各项阈值
     * @return 创建成功的猪舍对象，包含数据库生成的ID
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Pigsty> createPigsty(@RequestBody Pigsty pigsty) {
        Pigsty savedPigsty = pigstyRepository.save(pigsty);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedPigsty);
    }

    /**
     * 更新猪舍信息
     * 
     * 根据猪舍ID更新猪舍的基本信息和环境指标阈值配置。
     * 仅具有 ADMIN 角色的用户可以执行此操作。
     * 
     * 接口路径: PUT /api/pigsties/{id}
     * 
     * @param id 要更新的猪舍ID
     * @param pigstyDetails 新的猪舍信息对象
     * @return 更新成功的猪舍对象，找不到返回404
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
     * 删除猪舍
     * 
     * 根据猪舍ID从系统中删除猪舍记录。
     * 仅具有 ADMIN 角色的用户可以执行此操作。
     * 
     * 接口路径: DELETE /api/pigsties/{id}
     * 
     * @param id 要删除的猪舍ID
     * @return 删除成功返回204，找不到返回404
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