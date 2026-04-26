package com.example.dbrefresh.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for database operations
 * Fetches table names and copies data between databases
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseService {

    @Qualifier("sourceJdbcTemplate")
    private final JdbcTemplate sourceJdbcTemplate;

    @Qualifier("targetJdbcTemplate")
    private final JdbcTemplate targetJdbcTemplate;

    /**
     * Fetch all table names from the source database
     */
    public List<String> getAllTableNames() {
        try {
            String query = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE' " +
                    "ORDER BY TABLE_NAME";

            List<String> tables = sourceJdbcTemplate.queryForList(query, String.class);
            log.info("Found {} tables in source database", tables.size());
            return tables;
        } catch (Exception e) {
            log.error("Error fetching table names from source database", e);
            throw new RuntimeException("Failed to fetch table names", e);
        }
    }

    /**
     * Copy all data from source table to target table
     * Assumes both tables have identical structure
     *
     * @param tableName Name of the table to copy
     * @return Number of rows copied
     */
    public long copyTableData(String tableName) {
        try {
            log.info("Starting data copy for table: {}", tableName);

            // Clear target table first (optional - use with caution)
            String deleteQuery = "DELETE FROM " + tableName;
            targetJdbcTemplate.execute(deleteQuery);
            log.debug("Cleared target table: {}", tableName);

            // Get column names
            String columnsQuery = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?";
            List<String> columns = sourceJdbcTemplate.queryForList(columnsQuery, String.class, tableName);

            if (columns.isEmpty()) {
                log.warn("No columns found for table: {}", tableName);
                return 0;
            }

            String columnsList = String.join(",", columns);

            // Copy data using INSERT INTO ... SELECT
            String copyQuery = String.format(
                    "INSERT INTO %s (%s) SELECT %s FROM %s",
                    tableName, columnsList, columnsList, tableName
            );

            // Note: This query will copy from source to target
            // You might need to use linked server or a different approach
            // For now, we'll use a different strategy - copy via Java application
            long rowsCopied = copyDataViaApplication(tableName, columns);

            log.info("Successfully copied {} rows for table: {}", rowsCopied, tableName);
            return rowsCopied;
        } catch (Exception e) {
            log.error("Error copying data for table: {}", tableName, e);
            throw new RuntimeException("Failed to copy data for table: " + tableName, e);
        }
    }

    /**
     * Copy data from source to target database via application
     * This reads data from source and inserts into target
     */
    private long copyDataViaApplication(String tableName, List<String> columns) {
        try {
            String columnsList = String.join(",", columns);
            String placeholders = String.join(",", columns.stream().map(c -> "?").toList());

            // Fetch all data from source
            String selectQuery = String.format("SELECT * FROM %s", tableName);
            List<Object[]> sourceData = sourceJdbcTemplate.queryForList(selectQuery)
                    .stream()
                    .map(map -> columns.stream()
                            .map(col -> map.get(col.toLowerCase()))
                            .toArray())
                    .toList();

            if (sourceData.isEmpty()) {
                log.info("No data to copy for table: {}", tableName);
                return 0;
            }

            // Insert into target
            String insertQuery = String.format(
                    "INSERT INTO %s (%s) VALUES (%s)",
                    tableName, columnsList, placeholders
            );

            int[] results = targetJdbcTemplate.batchUpdate(insertQuery, sourceData);

            return results.length;
        } catch (Exception e) {
            log.error("Error in copyDataViaApplication for table: {}", tableName, e);
            throw new RuntimeException("Batch copy failed for table: " + tableName, e);
        }
    }

    /**
     * Get row count for a table
     */
    public long getTableRowCount(String tableName) {
        try {
            String query = String.format("SELECT COUNT(*) FROM %s", tableName);
            Long count = sourceJdbcTemplate.queryForObject(query, Long.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("Error getting row count for table: {}", tableName, e);
            return 0;
        }
    }
}

