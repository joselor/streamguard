package com.streamguard.queryapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * StreamGuard Query API - REST API for security event and AI analysis queries
 *
 * Provides HTTP endpoints to query:
 * - Security events stored in RocksDB
 * - AI threat analysis results
 * - Aggregated statistics and metrics
 *
 * @author Jose Ortuno
 * @version 1.0
 */
@SpringBootApplication
public class QueryApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(QueryApiApplication.class, args);
    }
}
