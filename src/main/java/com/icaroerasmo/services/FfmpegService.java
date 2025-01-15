package com.icaroerasmo.services;

import com.icaroerasmo.parsers.FfmpegStrParser;
import com.icaroerasmo.properties.JavaRtspProperties;
import com.icaroerasmo.properties.RtspProperties;
import com.icaroerasmo.properties.StorageProperties;
import com.icaroerasmo.runners.FfmpegRunner;
import com.icaroerasmo.storage.FutureStorage;
import com.icaroerasmo.util.FfmpegUtil;
import com.icaroerasmo.util.PropertiesUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Log4j2
@Service
@RequiredArgsConstructor
public class FfmpegService {

    private final JavaRtspProperties javaRtspProperties;
    private final ExecutorService executorService;
    private final FfmpegRunner ffmpegRunner;
    private final PropertiesUtil propertiesUtil;
    private final FutureStorage futureStorage;
    private final FfmpegUtil ffmpegUtil;

    @SneakyThrows
    @PostConstruct
    public void init() {

        log.info("Starting ffmpeg service");

        final StorageProperties storageProperties = javaRtspProperties.getStorageProperties();

        moveFilesToRecordsFolder(Arrays.asList(
                Objects.requireNonNull(Paths.get(
                        storageProperties.getTmpFolder()).toFile().list(
                                (dir, name) -> name.toLowerCase().endsWith(".mkv")))));

        final RtspProperties rtspProperties = javaRtspProperties.getRtspProperties();

        rtspProperties.getCameras().stream().
                map(this::parseCamInfo).
                forEach(this::ffmpegFutureSubmitter);

        log.info("All cameras started.");
    }

    private void moveFilesToRecordsFolder(List<String> fileNames) {

        final StorageProperties storageProperties = javaRtspProperties.getStorageProperties();
        final Path tmpFolder = Paths.get(storageProperties.getTmpFolder());

        fileNames.forEach(fileName -> {

            Map<String, String> dateMap = ffmpegUtil.extractInfoFromFileName(fileName);

            final Path destinationFolder =
                    Paths.get(storageProperties.getRecordsFolder(), dateMap.get("year"),
                            dateMap.get("month"), dateMap.get("day"), dateMap.get("hour"), dateMap.get("camName"));

            if(!destinationFolder.toFile().exists()) {
                destinationFolder.toFile().mkdirs();
            }

            final Path destinationPath = destinationFolder.resolve(fileName);

            try {
                Files.move(tmpFolder.resolve(fileName), destinationPath);
            } catch (Exception e) {
                log.error("Error moving file: {}", e.getMessage(), e);

            }
        });
    }

    public void start(String camName) {
        final RtspProperties rtspProperties = javaRtspProperties.getRtspProperties();

        if(futureStorage.get(camName) != null) {
            log.error("Camera {} already recording.", camName);
            throw new RuntimeException("Camera " + camName + " is already recording.");
        }

        rtspProperties.getCameras().stream().
                filter(camera -> camera.getName().equals(camName)).
                map(this::parseCamInfo).
                forEach(this::ffmpegFutureSubmitter);
    }

    public void stop(String camName) {

        Map<String, Future<Void>> futureMap =  futureStorage.get(camName);

        if(futureMap == null) {
            log.error("Camera {} not found", camName);
            throw new RuntimeException("Camera " + camName + " is not recording.");
        }

        futureMap.keySet().forEach(futureName -> futureMap.get(futureName).cancel(true));
    }

    @PreDestroy
    public void destroy() {
        log.info("Stopping ffmpeg service");
        executorService.shutdownNow();
    }

    private Map.Entry<String, String> parseCamInfo(RtspProperties.Camera camera) {
        final RtspProperties rtspProperties = javaRtspProperties.getRtspProperties();
        final StorageProperties storageProperties = javaRtspProperties.getStorageProperties();
        return Map.entry(camera.getName(),
                FfmpegStrParser.builder().
                        cameraName(camera.getName()).
                        timeout(rtspProperties.getTimeout()).
                        transportProtocol(rtspProperties.getTransportProtocol()).
                        url(propertiesUtil.cameraUrlParser(camera)).
                        doneSegmentsListSize(5).
                        tmpPath(storageProperties.getTmpFolder()).
                        videoDuration(rtspProperties.getVideoDuration()).build()
        );
    }

    private Future<Void> ffmpegFutureSubmitter(Map.Entry<String, String> entry) {
        Future<Void> future = executorService.submit(() -> ffmpegRunner.run(entry.getKey(), entry.getValue()));
        futureStorage.put(entry.getKey(), "main", future);
        return future;
    }
}
