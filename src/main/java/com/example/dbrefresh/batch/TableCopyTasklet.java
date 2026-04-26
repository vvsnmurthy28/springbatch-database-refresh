package com.example.dbrefresh.batch;

import com.example.dbrefresh.model.TableRefreshStatus;
import com.example.dbrefresh.service.DatabaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Tasklet to handle multi-threaded table data copying
 * Fetches all tables and copies them in parallel using thread pool
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TableCopyTasklet implements Tasklet {

    private final DatabaseService databaseService;
    private static final int THREAD_POOL_SIZE = 5; // Configurable thread pool size

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
            throws Exception {

        log.info("===== Starting Table Copy Tasklet =====");
        long startTime = System.currentTimeMillis();

        try {
            // Step 1: Fetch all table names from source database
            List<String> tableNames = databaseService.getAllTableNames();
            log.info("Processing {} tables", tableNames.size());

            if (tableNames.isEmpty()) {
                log.warn("No tables found in source database");
                return RepeatStatus.FINISHED;
            }

            // Step 2: Process tables using thread pool
            List<TableRefreshStatus> results = copyTablesInParallel(tableNames);

            // Step 3: Store results in execution context for email notification
            chunkContext.getStepContext()
                    .getStepExecution()
                    .getJobExecution()
                    .getExecutionContext()
                    .put("tableRefreshResults", results);

            // Log summary
            long successCount = results.stream().filter(TableRefreshStatus::isSuccess).count();
            long failureCount = results.size() - successCount;

            log.info("===== Table Copy Tasklet Summary =====");
            log.info("Total Tables: {}", results.size());
            log.info("Successful: {}", successCount);
            log.info("Failed: {}", failureCount);
            log.info("Total Time: {} ms", System.currentTimeMillis() - startTime);

            return RepeatStatus.FINISHED;
        } catch (Exception e) {
            log.error("Error in TableCopyTasklet", e);
            throw e;
        }
    }

    /**
     * Copy multiple tables in parallel using thread pool
     */
    private List<TableRefreshStatus> copyTablesInParallel(List<String> tableNames)
            throws InterruptedException {

        List<TableRefreshStatus> results = new CopyOnWriteArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        try {
            // Submit all table copy tasks
            for (String tableName : tableNames) {
                executor.submit(() -> {
                    TableRefreshStatus status = new TableRefreshStatus(tableName);
                    long taskStartTime = System.currentTimeMillis();

                    try {
                        // Copy table data
                        long rowsCopied = databaseService.copyTableData(tableName);
                        status.setRowsCopied(rowsCopied);
                        status.setSuccess(true);
                        log.info("✓ Table '{}' copied successfully ({} rows)",
                                tableName, rowsCopied);
                    } catch (Exception e) {
                        status.setSuccess(false);
                        status.setErrorMessage(e.getMessage());
                        log.error("✗ Failed to copy table '{}'", tableName, e);
                    } finally {
                        status.setExecutionTimeMs(System.currentTimeMillis() - taskStartTime);
                        results.add(status);
                    }
                });
            }

            // Wait for all tasks to complete
            executor.shutdown();
            boolean completed = executor.awaitTermination(30, TimeUnit.MINUTES);

            if (!completed) {
                log.warn("Thread pool did not terminate within timeout");
                executor.shutdownNow();
            }

            return results;
        } catch (Exception e) {
            executor.shutdownNow();
            throw new RuntimeException("Error in parallel table processing", e);
        }
    }
}

