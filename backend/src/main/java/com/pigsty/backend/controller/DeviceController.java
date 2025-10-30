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

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    @Autowired
    private DeviceRepository deviceRepository;

    @PersistenceContext // 3. 注入 EntityManager (更底层的数据库操作)
    private EntityManager entityManager;

    // --- 获取设备 (保持不变) ---
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Device>> getAllDevices(@RequestParam(required = false) Long pigstyId) {
        List<Device> devices;
        if (pigstyId != null) { devices = deviceRepository.findByPigstyId(pigstyId); }
        else { devices = deviceRepository.findAll(); }
        return ResponseEntity.ok(devices);
    }

    // --- 添加设备 (保持不变) ---
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Device> addDevice(@RequestBody Device device) {
        Device savedDevice = deviceRepository.save(device);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedDevice);
    }

    // --- 更新设备 (保持不变) ---
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
     * API 3.1: 切换设备状态 (更方便的前端接口)
     * [!!! 最终修复 !!!]
     * 使用 EntityManager 进行更底层的、显式的更新和刷新
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


    // --- 删除设备 (保持不变) ---
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