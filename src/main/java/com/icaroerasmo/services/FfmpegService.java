package com.icaroerasmo.services;

import com.icaroerasmo.parsers.FfmpegStrParser;
import com.icaroerasmo.properties.JavaRtspProperties;
import com.icaroerasmo.properties.RtspProperties;
import com.icaroerasmo.properties.StorageProperties;
import com.icaroerasmo.runners.FfmpegRunner;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
@RequiredArgsConstructor
public class FfmpegService {

    private final JavaRtspProperties javaRtspProperties;
    private final StorageProperties storageProperties;
    private final RtspProperties rtspProperties;

    @Autowired
    private StorageProperties storageProperties2;

    @Autowired
    private RtspProperties rtspProperties2;

    @PostConstruct
    public void init() {

        ExecutorService executorService = Executors.newFixedThreadPool(rtspProperties.getCameras().size());

        /*List<Future<Void>>*/ List<String> futures = rtspProperties.getCameras().stream().
                map(camera ->
                    FfmpegStrParser.builder().
                            cameraName(camera.getName()).
                            timeout(rtspProperties.getTimeout()).
                            url(cameraUrlParser(camera)).
                            doneSegmentsListSize(5).
                            tmpPath(storageProperties.getTmpFolder()).
                            videoDuration(storageProperties.getVideoDuration()).build()
                ).
//                map(command -> /*executorService.submit(() -> new FfmpegRunner(command).get())*/).
                toList();

    }

    private String cameraUrlParser(RtspProperties.Camera camera) {

        if(camera.getUrl() != null && !camera.getUrl().isBlank()) {
            return camera.getUrl();
        }

        return "rtsp://" + camera.getUsername()  + ":" + camera.getPassword() + "@" + camera.getHost() + ":" + camera.getPort() + "/"+camera.getFormat();
    }
}
