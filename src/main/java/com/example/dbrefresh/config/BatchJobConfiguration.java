package com.example.dbrefresh.config;

import com.example.dbrefresh.batch.TableCopyTasklet;
import com.example.dbrefresh.batch.TableRefreshJobListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Configuration for Spring Batch Job
 * Defines the job and step for database table refresh
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class BatchJobConfiguration {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final TableCopyTasklet tableCopyTasklet;
    private final TableRefreshJobListener jobListener;

    /**
     * Define the tasklet step that copies table data
     */
    @Bean
    public Step tableCopyStep() {
        log.debug("Creating tableCopyStep");
        return new StepBuilder("tableCopyStep", jobRepository)
                .tasklet(tableCopyTasklet, transactionManager)
                .build();
    }

    /**
     * Define the batch job
     */
    @Bean
    public Job tableRefreshJob(Step tableCopyStep) {
        log.debug("Creating tableRefreshJob");
        return new JobBuilder("tableRefreshJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(jobListener)
                .start(tableCopyStep)
                .build();
    }
}

