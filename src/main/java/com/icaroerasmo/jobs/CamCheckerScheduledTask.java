package com.icaroerasmo.jobs;

import com.icaroerasmo.enums.MessagesEnum;
import com.icaroerasmo.properties.JavaRtspProperties;
import com.icaroerasmo.properties.RtspProperties;
import com.icaroerasmo.properties.StorageProperties;
import com.icaroerasmo.services.FfmpegService;
import com.icaroerasmo.storage.FutureStorage;
import com.icaroerasmo.messaging.NotificationPublisher;
import com.icaroerasmo.util.PropertiesUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Log4j2
@Component
@RequiredArgsConstructor
public class CamCheckerScheduledTask {

    private final NotificationPublisher publisher;
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
                publisher.publishText(MessagesEnum.CAM_CHECKER_NOT_RUNNING, camName);
                futureStorage.delete(camName);
                ffmpegService.start(camName);
                continue;
            }

            // 2. Future is running — check if segmenter is actually producing segments
            // The segment list file (.camName_done_segments) only updates when a segment completes,
            // making it the true health indicator. The video file keeps getting written to even
            // when the segmenter is stuck, so checking its lastModified is unreliable.
            long segmentListAge = getSegmentListAge(camName, storageProperties.getTmpFolder());

            if (segmentListAge < 0) {
                log.debug("Cam {}: no segment list found yet, skipping staleness check", camName);
                continue;
            }

            if (segmentListAge > staleThresholdMs) {
                log.warn("Cam {} is running but segment list was last updated {}ms ago (threshold: {}ms). Killing zombie and restarting...",
                        camName, segmentListAge, staleThresholdMs);
                publisher.publishText(MessagesEnum.CAM_CHECKER_NOT_RECORDING, camName);

                Process zombieProcess = futureStorage.getProcess(camName);
                if (zombieProcess != null) {
                    log.warn("Cam {}: destroying zombie process pid {}", camName, zombieProcess.pid());
                    zombieProcess.destroyForcibly();
                }

                futureStorage.delete(camName);
                ffmpegService.start(camName);
            } else {
                log.debug("Cam {}: segment list updated {}ms ago — healthy", camName, segmentListAge);
            }
        }
    }

    private long getSegmentListAge(String camName, String tmpFolder) {
        Path segmentListPath = Path.of(tmpFolder, "." + camName + "_done_segments");
        File segmentListFile = segmentListPath.toFile();

        if (!segmentListFile.exists()) {
            return -1;
        }

        return System.currentTimeMillis() - segmentListFile.lastModified();
    }
}
