package com.pigsty.backend.controller;

import com.pigsty.backend.model.Device;
import com.pigsty.backend.repository.DeviceRepository;
import jakarta.persistence.EntityManager; // 1. 导入 EntityManager
import jakarta.persistence.PersistenceContext; // 2. 导入 PersistenceContext
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 设备管理控制器
 * 
 * 该控制器负责处理物联网设备的增删改查请求。
 * 所有接口路径都以 /api/devices 为前缀。
 * 
 * 主要功能：
 * - 获取设备列表：所有已认证用户可访问，支持按猪舍筛选
 * - 添加设备：仅 ADMIN 角色可操作
 * - 更新设备：仅 ADMIN 角色可操作
 * - 切换设备状态：仅 ADMIN 角色可操作
 * - 删除设备：仅 ADMIN 角色可操作
 * 
 * @author 系统架构
 * @version 1.0
 */
@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    @Autowired
    private DeviceRepository deviceRepository;

    @PersistenceContext // 3. 注入 EntityManager (更底层的数据库操作)
    private EntityManager entityManager;

    /**
     * 获取设备列表
     * 
     * 查询系统中的所有设备，支持按猪舍ID进行筛选。
     * 所有已登录认证的用户都可以访问此接口。
     * 
     * 接口路径: GET /api/devices
     * 
     * @param pigstyId 可选参数，用于筛选特定猪舍的设备
     * @return 设备列表，包含符合条件的所有设备信息
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Device>> getAllDevices(@RequestParam(required = false) Long pigstyId) {
        List<Device> devices;
        if (pigstyId != null) { devices = deviceRepository.findByPigstyId(pigstyId); }
        else { devices = deviceRepository.findAll(); }
        return ResponseEntity.ok(devices);
    }

    /**
     * 添加新设备
     * 
     * 在系统中创建一个新的物联网设备记录。
     * 仅具有 ADMIN 角色的用户可以执行此操作。
     * 
     * 接口路径: POST /api/devices
     * 
     * @param device 设备信息对象，包含猪舍ID、类型、型号等信息
     * @return 创建成功的设备对象，包含数据库生成的ID
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Device> addDevice(@RequestBody Device device) {
        Device savedDevice = deviceRepository.save(device);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedDevice);
    }

    /**
     * 更新设备信息
     * 
     * 根据设备ID更新设备的基本信息和状态。
     * 仅具有 ADMIN 角色的用户可以执行此操作。
     * 
     * 接口路径: PUT /api/devices/{id}
     * 
     * @param id 要更新的设备ID
     * @param deviceDetails 新的设备信息对象
     * @return 更新成功的设备对象，找不到返回404
     */
     @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Device> updateDevice(@PathVariable Long id, @RequestBody Device deviceDetails) {
        return deviceRepository.findById(id)
            .map(existingDevice -> {
                existingDevice.setActive(deviceDetails.isActive());
                existingDevice.setModelNumber(deviceDetails.getModelNumber());
                existingDevice.setSerialNumber(deviceDetails.getSerialNumber());
                Device updatedDevice = deviceRepository.save(existingDevice);
                return ResponseEntity.ok(updatedDevice);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 切换设备状态
     * 
     * 切换设备的启用/停用状态。
     * 使用 EntityManager 进行更底层的、显式的更新和刷新操作。
     * 仅具有 ADMIN 角色的用户可以执行此操作。
     * 
     * 接口路径: POST /api/devices/{id}/toggle
     * 
     * @param id 要切换状态的设备ID
     * @return 更新后的设备对象，找不到返回404
     */
    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional // 仍然需要事务
    public ResponseEntity<Device> toggleDeviceStatus(@PathVariable Long id) {
        Optional<Device> deviceOpt = deviceRepository.findById(id);

        if (deviceOpt.isPresent()) {
            Device existingDevice = deviceOpt.get();
            boolean newState = !existingDevice.isActive(); // 计算新状态

            // 4. [最关键的改动] 使用 EntityManager 直接更新数据库
            existingDevice.setActive(newState);
            entityManager.merge(existingDevice); // 合并更改到持久化上下文
            entityManager.flush(); // 强制将更改写入数据库
            entityManager.refresh(existingDevice); // 从数据库重新读取对象，确保拿到最新状态

            System.out.println("Toggling device ID: " + id + ", New state from DB: " + existingDevice.isActive());

            // 5. 返回从数据库 *刷新* 后的对象
            return ResponseEntity.ok(existingDevice);
        } else {
            return ResponseEntity.notFound().build();
        }
    }


    /**
     * 删除设备
     * 
     * 根据设备ID从系统中删除设备记录。
     * 仅具有 ADMIN 角色的用户可以执行此操作。
     * 
     * 接口路径: DELETE /api/devices/{id}
     * 
     * @param id 要删除的设备ID
     * @return 删除成功返回204，找不到返回404
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id) {
        if (deviceRepository.existsById(id)) {
            deviceRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}