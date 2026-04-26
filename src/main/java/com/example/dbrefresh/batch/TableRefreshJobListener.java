package com.example.dbrefresh.batch;

import com.example.dbrefresh.model.TableRefreshStatus;
import com.example.dbrefresh.service.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Listener to handle job execution completion
 * Sends email notification with job summary
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TableRefreshJobListener extends JobExecutionListenerSupport {

    private final EmailNotificationService emailNotificationService;

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("===== Batch Job Completed Successfully =====");
            
            // Extract results from execution context
            List<TableRefreshStatus> results = 
                    (List<TableRefreshStatus>) jobExecution
                            .getExecutionContext()
                            .get("tableRefreshResults");

            if (results != null && !results.isEmpty()) {
                long totalTime = Duration.between(jobExecution.getStartTime(), jobExecution.getEndTime()).toMillis();
                
                // Send email notification
                emailNotificationService.sendJobCompletionEmail(results, totalTime);
            }
        } else if (jobExecution.getStatus() == BatchStatus.FAILED) {
            log.error("===== Batch Job Failed =====");
            log.error("Job failed with exceptions:");
            jobExecution.getAllFailureExceptions().forEach(e -> 
                    log.error("Exception: ", e));
        }
    }
}

