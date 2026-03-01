package com.pigsty.backend.repository;

import com.pigsty.backend.model.WarningLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarningLogRepository extends JpaRepository<WarningLog, Long> {

    @Query("""
        SELECT w FROM WarningLog w
        WHERE w.acknowledged = :acknowledged
          AND (:pigstyId IS NULL OR w.pigstyId = :pigstyId)
          AND (:metricType IS NULL OR w.metricType = :metricType)
    """)
    Page<WarningLog> searchForAdmin(boolean acknowledged, String pigstyId, String metricType, Pageable pageable);

    @Query("""
        SELECT w FROM WarningLog w
        WHERE w.acknowledged = :acknowledged
          AND w.pigstyId IN :pigstyIds
          AND (:metricType IS NULL OR w.metricType = :metricType)
    """)
    Page<WarningLog> searchForTechnician(boolean acknowledged, List<String> pigstyIds, String metricType, Pageable pageable);
}
