package com.example.dbrefresh.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean(name = "dataSource")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource dataSource() {
        return new HikariDataSource();
    }

    /**
     * Primary DataSource - Source Database (PROD)
     */
    @Bean(name = "sourceDataSource")
    @ConfigurationProperties(prefix = "source.datasource")
    public DataSource sourceDataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * Secondary DataSource - Target Database (LOWER ENV)
     */
    @Bean(name = "targetDataSource")
    @ConfigurationProperties(prefix = "target.datasource")
    public DataSource targetDataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * JdbcTemplate for Source Database
     */
    @Bean(name = "sourceJdbcTemplate")
    public JdbcTemplate sourceJdbcTemplate(
            @Qualifier("sourceDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * JdbcTemplate for Target Database
     */
    @Bean(name = "targetJdbcTemplate")
    public JdbcTemplate targetJdbcTemplate(
            @Qualifier("targetDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}

