package com.icaroerasmo.jobs;

import com.icaroerasmo.runners.RcloneDedupeRunner;
import com.icaroerasmo.storage.FutureStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Component
@RequiredArgsConstructor
public class RcloneDedupeScheduledTask {

    private final RcloneDedupeRunner rcloneDedupeRunner;
    private final FutureStorage futureStorage;
    private final ExecutorService executorService;

    @Scheduled(cron = "#{@rcloneProperties.dedupeCron}")
    private void rcloneDedupe() {
        Future<Void> future = executorService.submit(rcloneDedupeRunner::run);
        futureStorage.put("rclone", "dedupe", future);
    }
}
