package com.example.dbrefresh.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Scheduler for periodic batch job execution
 * Runs the table refresh job daily at 6 AM
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchJobScheduler {

    private final JobLauncher jobLauncher;
    private final Job tableRefreshJob;

    /**
     * Scheduled method to run batch job daily at 6 AM
     * Cron expression: 0 0 6 * * * (6 AM every day)
     * You can modify the cron expression as needed
     */
    @Scheduled(cron = "0 0 6 * * *")
    public void runTableRefreshJob() {
        log.info("===== Starting scheduled Table Refresh Job =====");

        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addDate("startTime", new Date())
                    .toJobParameters();

            jobLauncher.run(tableRefreshJob, jobParameters);
            log.info("Batch job launched successfully");
        } catch (Exception e) {
            log.error("Error launching batch job", e);
        }
    }

    /**
     * Manual trigger for testing - runs via REST endpoint (optional)
     * Can be called via GET /api/batch/trigger
     */
    public void triggerJobManually() {
        log.info("===== Manually triggered Table Refresh Job =====");

        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addDate("startTime", new Date())
                    .toJobParameters();

            jobLauncher.run(tableRefreshJob, jobParameters);
            log.info("Batch job launched manually");
        } catch (Exception e) {
            log.error("Error launching batch job", e);
        }
    }
}
