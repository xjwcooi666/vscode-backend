package com.pigsty.backend.controller;

import com.pigsty.backend.model.EnvironmentalData;
import com.pigsty.backend.repository.DataRepository;
import com.pigsty.backend.service.WarningService; // 1. 导入 WarningService
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/data")
public class DataController {

    @Autowired
    private DataRepository dataRepository;

    @Autowired // 2. 注入 WarningService
    private WarningService warningService; 

    @PostMapping
    public ResponseEntity<EnvironmentalData> addData(@RequestBody EnvironmentalData data) {
        // 步骤 A: 先保存数据
        EnvironmentalData savedData = dataRepository.save(data);

        // 步骤 B: (新！)调用服务，检查预警
        warningService.checkDataForWarnings(savedData); 

        return ResponseEntity.ok(savedData); // 返回保存后的数据
    }

    // ... (getLatestData 和 getDataByPigstyId 方法保持不变) ...
    
    @GetMapping("/latest")
    public ResponseEntity<List<EnvironmentalData>> getLatestData() {
        List<EnvironmentalData> dataList = dataRepository.findTop100ByOrderByTimestampDesc();
        return ResponseEntity.ok(dataList);
    }

    @GetMapping("/pigsty/{pigstyId}")
    public ResponseEntity<List<EnvironmentalData>> getDataByPigstyId(@PathVariable String pigstyId) {
        List<EnvironmentalData> dataList = dataRepository.findByPigstyIdOrderByTimestampDesc(pigstyId);
        return ResponseEntity.ok(dataList);
    }
}
