package com.icaroerasmo.config;

import com.icaroerasmo.properties.RcloneProperties;
import com.icaroerasmo.services.SyncStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Exposes the last sync completion timestamp so the dashboard can alert when the
 * sync is overdue (or the recorder is unreachable).
 */
@RestController
@RequestMapping("/actuator/sync")
@RequiredArgsConstructor
public class SyncController {

    private final SyncStateService syncStateService;
    private final RcloneProperties rcloneProperties;

    @GetMapping
    public Map<String, Object> sync() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("lastSyncEpochMillis", syncStateService.lastSyncEpochMillis());
        result.put("syncIntervalMinutes", rcloneProperties.getSyncIntervalMinutes());
        return result;
    }
}
