package com.icaroerasmo.jobs;

import com.icaroerasmo.runners.RcloneSyncRunner;
import com.icaroerasmo.storage.FutureStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Component
@RequiredArgsConstructor
public class RcloneSyncScheduledTask {

    private final ExecutorService executorService;
    private final RcloneSyncRunner rcloneSyncRunner;
    private final FutureStorage futureStorage;

    @Scheduled(fixedRateString = "#{@rcloneProperties.syncIntervalMinutes * 60 * 1000}")
    private void rcloneSync() {
        Future<Void> future = executorService.submit(rcloneSyncRunner::run);
        futureStorage.put("rclone", "sync", future);
    }
}
