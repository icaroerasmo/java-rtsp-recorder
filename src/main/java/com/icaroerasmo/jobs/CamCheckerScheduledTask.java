package com.icaroerasmo.jobs;

import com.icaroerasmo.enums.MessagesEnum;
import com.icaroerasmo.properties.JavaRtspProperties;
import com.icaroerasmo.properties.RtspProperties;
import com.icaroerasmo.properties.StorageProperties;
import com.icaroerasmo.services.FfmpegService;
import com.icaroerasmo.storage.FutureStorage;
import com.icaroerasmo.util.PropertiesUtil;
import com.icaroerasmo.util.TelegramUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Log4j2
@Component
@RequiredArgsConstructor
public class CamCheckerScheduledTask {

    private final TelegramUtil telegramUtil;
    private final JavaRtspProperties javaRtspProperties;
    private final FutureStorage futureStorage;
    private final FfmpegService ffmpegService;
    private final PropertiesUtil propertiesUtil;

    @Scheduled(fixedDelay = 60000)
    public void checkIfCamsAreOnline() {
        RtspProperties rtspProperties = javaRtspProperties.getRtspProperties();
        StorageProperties storageProperties = javaRtspProperties.getStorageProperties();
        long videoDurationMs = Long.parseLong(propertiesUtil.durationParser(rtspProperties.getVideoDuration(), TimeUnit.MILLISECONDS));
        long staleThresholdMs = videoDurationMs * 3;

        for (RtspProperties.Camera camera : rtspProperties.getCameras()) {
            String camName = camera.getName();

            // 1. Check if the Future is running
            if (!futureStorage.isRunning(camName)) {
                log.warn("Cam {} is not running (future not active)...", camName);
                telegramUtil.sendMessage(MessagesEnum.CAM_CHECKER_NOT_RUNNING, camName);
                futureStorage.delete(camName);
                ffmpegService.start(camName);
                continue;
            }

            // 2. Future is running — check if ffmpeg is actually producing output
            long lastModified = getLastModifiedForCamera(camName, storageProperties.getTmpFolder());

            if (lastModified < 0) {
                // No files found at all — camera just started or no segments yet, give it time
                log.debug("Cam {}: no recording files found in tmp, skipping staleness check", camName);
                continue;
            }

            long staleAge = System.currentTimeMillis() - lastModified;

            if (staleAge > staleThresholdMs) {
                log.warn("Cam {} is running but last recording was {}ms ago (threshold: {}ms). Killing zombie and restarting...",
                        camName, staleAge, staleThresholdMs);
                telegramUtil.sendMessage(MessagesEnum.CAM_CHECKER_NOT_RECORDING, camName);

                // Kill the zombie process
                Process zombieProcess = futureStorage.getProcess(camName);
                if (zombieProcess != null) {
                    log.warn("Cam {}: destroying zombie process pid {}", camName, zombieProcess.pid());
                    zombieProcess.destroyForcibly();
                }

                futureStorage.delete(camName);
                ffmpegService.start(camName);
            } else {
                log.debug("Cam {}: last recording updated {}ms ago — healthy", camName, staleAge);
            }
        }
    }

    private long getLastModifiedForCamera(String camName, String tmpFolder) {
        Path tmpPath = Path.of(tmpFolder);
        if (!Files.isDirectory(tmpPath)) {
            return -1;
        }

        long newestModified = -1;

        File[] files = tmpPath.toFile().listFiles((dir, name) ->
                name.startsWith(camName) && name.endsWith(".mkv"));

        if (files == null || files.length == 0) {
            return -1;
        }

        for (File file : files) {
            long modified = file.lastModified();
            if (modified > newestModified) {
                newestModified = modified;
            }
        }

        return newestModified;
    }
}
