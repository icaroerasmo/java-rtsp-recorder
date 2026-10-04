package com.icaroerasmo.services;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Holds the timestamp of the last rclone sync completion so the dashboard can
 * detect when the recorder is up but the sync stopped happening.
 */
@Component
public class SyncStateService {

    private final AtomicLong lastSyncEpochMillis = new AtomicLong(0);

    public void recordSync() {
        lastSyncEpochMillis.set(System.currentTimeMillis());
    }

    public long lastSyncEpochMillis() {
        return lastSyncEpochMillis.get();
    }
}
