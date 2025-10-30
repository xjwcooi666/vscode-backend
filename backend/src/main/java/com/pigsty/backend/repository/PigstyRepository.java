package com.pigsty.backend.repository;

import com.pigsty.backend.model.Pigsty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PigstyRepository extends JpaRepository<Pigsty, Long> {

    // 允许我们按技术员 ID 查找猪舍
    List<Pigsty> findByTechnicianId(Long technicianId);
}
