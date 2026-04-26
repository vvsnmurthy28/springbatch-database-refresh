package com.example.dbrefresh.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DTO for table refresh status
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableRefreshStatus implements Serializable {
    private String tableName;
    private long rowsCopied;
    private boolean success;
    private String errorMessage;
    private long executionTimeMs;

    public TableRefreshStatus(String tableName) {
        this.tableName = tableName;
        this.success = true;
        this.rowsCopied = 0;
        this.executionTimeMs = 0;
    }
}

