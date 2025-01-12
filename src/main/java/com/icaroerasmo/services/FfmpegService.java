package com.icaroerasmo.services;

import com.icaroerasmo.parsers.FfmpegStrParser;
import com.icaroerasmo.properties.JavaRtspProperties;
import com.icaroerasmo.properties.RtspProperties;
import com.icaroerasmo.properties.StorageProperties;
import com.icaroerasmo.runners.FfmpegRunner;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

@Log4j2
@Service
@RequiredArgsConstructor
public class FfmpegService {

    private final JavaRtspProperties javaRtspProperties;

    @PostConstruct
    public void init() {

        final RtspProperties rtspProperties = javaRtspProperties.getRtspProperties();
        final StorageProperties storageProperties = javaRtspProperties.getStorageProperties();

        ExecutorService executorService = Executors.newFixedThreadPool(rtspProperties.getCameras().size()*3);

        List<Future<Void>> futures = rtspProperties.getCameras().stream().
                map(camera ->
                    FfmpegStrParser.builder().
                            cameraName(camera.getName()).
                            timeout(rtspProperties.getTimeout()).
                            url(cameraUrlParser(camera)).
                            doneSegmentsListSize(5).
                            tmpPath(storageProperties.getTmpFolder()).
                            videoDuration(storageProperties.getVideoDuration()).build()
                ).
                map(command -> executorService.submit(() -> new FfmpegRunner(command, log::info, log::error).get())).
                toList();

    }

    private String cameraUrlParser(RtspProperties.Camera camera) {

        if(camera.getUrl() != null && !camera.getUrl().isBlank()) {
            return camera.getUrl();
        }

        return "rtsp://" + camera.getUsername()  + ":" + camera.getPassword() + "@" + camera.getHost() + ":" + camera.getPort() + "/"+camera.getFormat();
    }
}
