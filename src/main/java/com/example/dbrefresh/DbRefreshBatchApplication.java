package com.example.dbrefresh;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableBatchProcessing
public class DbRefreshBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(DbRefreshBatchApplication.class, args);
    }

}

