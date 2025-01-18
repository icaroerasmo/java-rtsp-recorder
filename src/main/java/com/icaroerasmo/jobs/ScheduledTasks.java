package com.icaroerasmo.jobs;

import com.icaroerasmo.properties.JavaRtspProperties;
import com.icaroerasmo.properties.RtspProperties;
import com.icaroerasmo.properties.StorageProperties;
import com.icaroerasmo.util.FfmpegUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Log4j2
@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private final FfmpegUtil ffmpegUtil;
    private final JavaRtspProperties javaRtspProperties;

    @Scheduled(fixedDelayString =
            "#{@propertiesUtil.durationParser(" +
            "@storageProperties.fileMoverSleep, " +
            "T(java.util.concurrent.TimeUnit).MILLISECONDS)}")
    private void filesMover() {

        log.info("Started job to move files to records folder");

        final StorageProperties storageProperties = javaRtspProperties.getStorageProperties();
        final List<RtspProperties.Camera> cameras = javaRtspProperties.getRtspProperties().getCameras();

        cameras.forEach(cam -> {
            final Path tmpFolder = Paths.get(storageProperties.getTmpFolder());
            final Path segmentsFile = tmpFolder.resolve(".%s_done_segments".formatted(cam.getName()));
            try {

                List<String> fileList = Files.readAllLines(segmentsFile);

                if(fileList.isEmpty()) {
                    return;
                }

                ffmpegUtil.moveFilesToRecordsFolder(fileList);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        ffmpegUtil.deleteEmptyFolders(Paths.get(storageProperties.getRecordsFolder()));

        log.info("Finished job to move files to records folder");
    }
}
