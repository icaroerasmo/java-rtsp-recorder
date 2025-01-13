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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

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
        final StorageProperties storageProperties = javaRtspProperties.getStorageProperties();

        List<Future<Void>> mainFutures = rtspProperties.getCameras().stream().
                map(camera ->
                    Map.entry(camera.getName(),
                        FfmpegStrParser.builder().
                                cameraName(camera.getName()).
                                timeout(rtspProperties.getTimeout()).
                                url(propertiesUtil.cameraUrlParser(camera)).
                                doneSegmentsListSize(5).
                                tmpPath(storageProperties.getTmpFolder()).
                                videoDuration(storageProperties.getVideoDuration()).build()
                    )
                ).
                map(entry -> {
                    Future<Void> future = executorService.submit(() -> ffmpegRunner.run(entry.getKey(), entry.getValue()));
                    futureStorage.put(entry.getKey(), "main", future);
                    return future;
                }).
                toList();

        log.info("All cameras started.");
    }

    @PreDestroy
    public void destroy() {
        log.info("Stopping ffmpeg service");
        executorService.shutdownNow();
    }
}
