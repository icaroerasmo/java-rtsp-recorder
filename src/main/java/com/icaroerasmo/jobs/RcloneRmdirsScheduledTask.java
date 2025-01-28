package com.icaroerasmo.jobs;

import com.icaroerasmo.runners.RcloneRmdirsRunner;
import com.icaroerasmo.storage.FutureStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Component
@RequiredArgsConstructor
public class RcloneRmdirsScheduledTask {

    private final RcloneRmdirsRunner rcloneRmdirsRunner;
    private final FutureStorage futureStorage;
    private final ExecutorService executorService;

    @Scheduled(cron = "#{@rcloneProperties.rmdirsCron}")
    private void rcloneRmdirs() {
        Future<Void> future = executorService.submit(rcloneRmdirsRunner::run);
        futureStorage.put("rclone", "main", future);
    }
}
