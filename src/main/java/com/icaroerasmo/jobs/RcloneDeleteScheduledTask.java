package com.icaroerasmo.jobs;

import com.icaroerasmo.runners.RcloneDeleteRunner;
import com.icaroerasmo.storage.FutureStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Component
@RequiredArgsConstructor
public class RcloneDeleteScheduledTask {

    private final RcloneDeleteRunner rcloneDeleteRunner;
    private final FutureStorage futureStorage;
    private final ExecutorService executorService;

    @Scheduled(cron = "#{@rcloneProperties.deleteCron}")
    private void rcloneDelete() {
        Future<Void> future = executorService.submit(rcloneDeleteRunner::run);
        futureStorage.put("rclone", "main", future);
    }
}
