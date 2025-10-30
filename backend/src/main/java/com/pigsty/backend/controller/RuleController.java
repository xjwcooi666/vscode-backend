package com.pigsty.backend.controller;

import com.pigsty.backend.model.WarningRule;
import com.pigsty.backend.repository.WarningRuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rules") // 所有规则相关的 API 都在 /api/rules 下
public class RuleController {

    @Autowired
    private WarningRuleRepository ruleRepository;

    /**
     * API 1: 创建一条新规则
     * POST /api/rules
     */
    @PostMapping
    public ResponseEntity<WarningRule> createRule(@RequestBody WarningRule rule) {
        WarningRule savedRule = ruleRepository.save(rule);
        return ResponseEntity.ok(savedRule);
    }

    /**
     * API 2: 获取所有规则
     * GET /api/rules
     */
    @GetMapping
    public ResponseEntity<List<WarningRule>> getAllRules() {
        List<WarningRule> rules = ruleRepository.findAll();
        return ResponseEntity.ok(rules);
    }

    /**
     * API 3: 删除一条规则 (按 ID)
     * DELETE /api/rules/1
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        if (ruleRepository.existsById(id)) {
            ruleRepository.deleteById(id);
            return ResponseEntity.ok().build(); // 成功删除
        } else {
            return ResponseEntity.notFound().build(); // 找不到
        }
    }
}
