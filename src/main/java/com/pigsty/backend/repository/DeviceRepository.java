package com.pigsty.backend.repository;

import com.pigsty.backend.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {

    // 允许我们按猪舍 ID 查找所有设备
    List<Device> findByPigstyId(Long pigstyId);
}
