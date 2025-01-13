package com.icaroerasmo.services;

import com.icaroerasmo.parsers.FfmpegStrParser;
import com.icaroerasmo.properties.JavaRtspProperties;
import com.icaroerasmo.properties.RtspProperties;
import com.icaroerasmo.properties.StorageProperties;
import com.icaroerasmo.runners.FfmpegRunner;
import com.icaroerasmo.storage.FutureStorage;
import com.icaroerasmo.util.PropertiesUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.Map;
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

    @PostConstruct
    public void init() {

        log.info("Starting ffmpeg service");

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
                        url(propertiesUtil.cameraUrlParser(camera)).
                        doneSegmentsListSize(5).
                        tmpPath(storageProperties.getTmpFolder()).
                        videoDuration(storageProperties.getVideoDuration()).build()
        );
    }

    private Future<Void> ffmpegFutureSubmitter(Map.Entry<String, String> entry) {
        Future<Void> future = executorService.submit(() -> ffmpegRunner.run(entry.getKey(), entry.getValue()));
        futureStorage.put(entry.getKey(), "main", future);
        return future;
    }
}
