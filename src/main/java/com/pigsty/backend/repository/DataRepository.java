package com.pigsty.backend.repository;

import com.pigsty.backend.model.EnvironmentalData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataRepository extends JpaRepository<EnvironmentalData, Long> {

    // 查找最新的 100 条数据
    List<EnvironmentalData> findTop100ByOrderByTimestampDesc();

    // 查找特定猪舍的数据
    List<EnvironmentalData> findByPigstyIdOrderByTimestampDesc(String pigstyId);
}
