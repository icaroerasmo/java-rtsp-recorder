package com.icaroerasmo.services;

import com.icaroerasmo.enums.MessagesEnum;
import com.icaroerasmo.parsers.FfmpegCommandParser;
import com.icaroerasmo.properties.JavaRtspProperties;
import com.icaroerasmo.properties.RtspProperties;
import com.icaroerasmo.properties.StorageProperties;
import com.icaroerasmo.runners.FfmpegRunner;
import com.icaroerasmo.storage.FutureStorage;
import com.icaroerasmo.util.FfmpegUtil;
import com.icaroerasmo.util.PropertiesUtil;
import com.icaroerasmo.util.TelegramUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Stream;

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
    private final TelegramUtil telegramUtil;

    @SneakyThrows
    @PostConstruct
    public void init() {

        log.info("Starting ffmpeg service");

        final StorageProperties storageProperties = javaRtspProperties.getStorageProperties();

        try (Stream<Path> allFiles = Files.list(Paths.get(storageProperties.getTmpFolder()))) {

            final List<String> mkvFiles = allFiles.
                    filter(f -> f.toString().endsWith(".mkv")).
                    map(f -> f.getName(f.getNameCount()-1).toString()).
                    toList();

            ffmpegUtil.moveFilesToRecordsFolder(mkvFiles);

        } catch (Exception e) {
            log.error("Error listing files in tmp folder: {}", e.getMessage());
            log.debug("Error listing files in tmp folder: {}", e.getMessage(), e);
            throw new RuntimeException("Error listing files in tmp folder", e);
        }

        final RtspProperties rtspProperties = javaRtspProperties.getRtspProperties();

        rtspProperties.getCameras().stream().
                map(this::parseCamInfo).
                forEach(this::ffmpegFutureSubmitter);

        log.info("All cameras started.");
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

        Map<String, Future<?>> futureMap =  futureStorage.get(camName);

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

    private Map.Entry<String, FfmpegCommandParser.FfmpegCommandParserBuilder> parseCamInfo(RtspProperties.Camera camera) {
        final RtspProperties rtspProperties = javaRtspProperties.getRtspProperties();
        final StorageProperties storageProperties = javaRtspProperties.getStorageProperties();
        return Map.entry(camera.getName(),
                FfmpegCommandParser.builder().
                        cameraName(camera.getName()).
                        transportProtocol(camera.getProtocol()).
                        url(propertiesUtil.cameraUrlParser(camera)).
                        doneSegmentsListSize(20).
                        tmpPath(storageProperties.getTmpFolder()).
                        timeout(rtspProperties.getTimeout()).
                        videoDuration(rtspProperties.getVideoDuration())
        );
    }

    @SneakyThrows
    private void ffmpegFutureSubmitter(Map.Entry<String, FfmpegCommandParser.FfmpegCommandParserBuilder> entry) {

        log.info("Camera {} initiating...", entry.getKey());
        telegramUtil.sendMessage(MessagesEnum.CAM_INITIATING, entry.getKey());

        Future<Void> future = executorService.submit(() -> ffmpegRunner.run(entry.getKey(), entry.getValue()));
        futureStorage.put(entry.getKey(), "main", future);

    }
}
