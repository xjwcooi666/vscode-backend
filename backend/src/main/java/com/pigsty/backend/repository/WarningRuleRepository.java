package com.pigsty.backend.repository;

import com.pigsty.backend.model.WarningRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarningRuleRepository extends JpaRepository<WarningRule, Long> {

    // 查找所有启用的规则
    List<WarningRule> findByEnabledTrue();
}
