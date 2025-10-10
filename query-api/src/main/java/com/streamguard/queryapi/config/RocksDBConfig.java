package com.streamguard.queryapi.config;

import lombok.extern.slf4j.Slf4j;
import org.rocksdb.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;

/**
 * RocksDB Configuration
 *
 * Opens RocksDB in read-only mode with column families for:
 * - default: Security events
 * - ai_analysis: AI threat analysis results
 */
@Slf4j
@Configuration
public class RocksDBConfig {

    @Value("${rocksdb.path}")
    private String dbPath;

    @Value("${rocksdb.read-only:true}")
    private boolean readOnly;

    private RocksDB db;
    private final List<ColumnFamilyHandle> columnFamilyHandles = new ArrayList<>();

    @Bean
    public RocksDB rocksDB() throws RocksDBException {
        RocksDB.loadLibrary();

        // List existing column families
        List<byte[]> cfNames;
        try (Options options = new Options()) {
            cfNames = RocksDB.listColumnFamilies(options, dbPath);
        }

        if (cfNames.isEmpty()) {
            cfNames.add(RocksDB.DEFAULT_COLUMN_FAMILY);
        }

        // Create column family descriptors
        List<ColumnFamilyDescriptor> cfDescriptors = new ArrayList<>();
        for (byte[] cfName : cfNames) {
            cfDescriptors.add(new ColumnFamilyDescriptor(cfName, new ColumnFamilyOptions()));
        }

        // Open database in read-only mode
        DBOptions dbOptions = new DBOptions()
                .setCreateIfMissing(false)
                .setCreateMissingColumnFamilies(false);

        if (readOnly) {
            db = RocksDB.openReadOnly(dbOptions, dbPath, cfDescriptors, columnFamilyHandles);
            log.info("Opened RocksDB in read-only mode: {}", dbPath);
        } else {
            db = RocksDB.open(dbOptions, dbPath, cfDescriptors, columnFamilyHandles);
            log.info("Opened RocksDB in read-write mode: {}", dbPath);
        }

        // Log column families
        for (int i = 0; i < columnFamilyHandles.size(); i++) {
            String cfName = new String(cfDescriptors.get(i).getName());
            log.info("Column family [{}]: {}", i, cfName);
        }

        return db;
    }

    @Bean
    public ColumnFamilyHandle defaultColumnFamily() {
        return columnFamilyHandles.get(0); // default CF
    }

    @Bean
    public ColumnFamilyHandle aiAnalysisColumnFamily() {
        // Find ai_analysis column family
        for (int i = 0; i < columnFamilyHandles.size(); i++) {
            try {
                String name = new String(columnFamilyHandles.get(i).getName());
                if ("ai_analysis".equals(name)) {
                    log.info("Found ai_analysis column family at index {}", i);
                    return columnFamilyHandles.get(i);
                }
            } catch (RocksDBException e) {
                log.error("Error reading column family name", e);
            }
        }
        log.warn("ai_analysis column family not found, returning null");
        return null;
    }

    @PreDestroy
    public void closeDB() {
        if (db != null) {
            for (ColumnFamilyHandle handle : columnFamilyHandles) {
                handle.close();
            }
            db.close();
            log.info("Closed RocksDB");
        }
    }
}
