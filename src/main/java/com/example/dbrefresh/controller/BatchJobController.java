package com.example.dbrefresh.controller;

import com.example.dbrefresh.scheduler.BatchJobScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for batch job management
 * Provides endpoints to manually trigger and monitor batch jobs
 */
@Slf4j
@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
public class BatchJobController {

    private final BatchJobScheduler batchJobScheduler;

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        response.put("service", "DB Refresh Batch");
        return ResponseEntity.ok(response);
    }

    /**
     * Manually trigger the batch job
     * POST /api/batch/trigger
     */
    @PostMapping("/trigger")
    public ResponseEntity<Map<String, String>> triggerBatchJob() {
        try {
            batchJobScheduler.triggerJobManually();
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "Job triggered successfully");
            response.put("message", "Table refresh batch job has been started");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error triggering batch job", e);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "Error");
            response.put("message", "Failed to trigger batch job: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get job information
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, String>> getJobInfo() {
        Map<String, String> response = new HashMap<>();
        response.put("jobName", "tableRefreshJob");
        response.put("description", "Batch job to copy data from production DB to lower environment DB");
        response.put("schedule", "Daily at 6:00 AM");
        response.put("threadPoolSize", "5");
        
        return ResponseEntity.ok(response);
    }
}

