package com.pigsty.backend.repository;

import com.pigsty.backend.model.WarningLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarningLogRepository extends JpaRepository<WarningLog, Long> {

    // 查找最新的 100 条未处理的预警
    List<WarningLog> findTop100ByAcknowledgedFalseOrderByTimestampDesc();
}
