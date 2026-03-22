package com.pigsty.backend.controller;

import com.pigsty.backend.model.Device;
import com.pigsty.backend.repository.DeviceRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    @Autowired
    private DeviceRepository deviceRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Device>> getAllDevices(@RequestParam(required = false) Long pigstyId) {
        List<Device> devices;
        if (pigstyId != null) { devices = deviceRepository.findByPigstyId(pigstyId); }
        else { devices = deviceRepository.findAll(); }
        return ResponseEntity.ok(devices);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Device> addDevice(@RequestBody Device device) {
        Device savedDevice = deviceRepository.save(device);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedDevice);
    }

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

    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Device> toggleDeviceStatus(@PathVariable Long id) {
        Optional<Device> deviceOpt = deviceRepository.findById(id);

        if (deviceOpt.isPresent()) {
            Device existingDevice = deviceOpt.get();
            boolean newState = !existingDevice.isActive();

            existingDevice.setActive(newState);
            entityManager.merge(existingDevice);
            entityManager.flush();
            entityManager.refresh(existingDevice);

            System.out.println("Toggling device ID: " + id + ", New state from DB: " + existingDevice.isActive());

            return ResponseEntity.ok(existingDevice);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/reset")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Map<String, Object>> resetDevice(@PathVariable Long id) {
        Optional<Device> deviceOpt = deviceRepository.findById(id);

        if (deviceOpt.isPresent()) {
            Device device = deviceOpt.get();
            device.setOperatingStatus("online");
            device.setLastHeartbeat(LocalDateTime.now());
            
            entityManager.merge(device);
            entityManager.flush();
            entityManager.refresh(device);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "设备复位成功");
            result.put("device", device);

            return ResponseEntity.ok(result);
        } else {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "设备不存在");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResult);
        }
    }

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