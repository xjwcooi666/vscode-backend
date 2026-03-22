package com.pigsty.backend.repository;

import com.pigsty.backend.model.WarningLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WarningRepository extends JpaRepository<WarningLog, Long> {
}
