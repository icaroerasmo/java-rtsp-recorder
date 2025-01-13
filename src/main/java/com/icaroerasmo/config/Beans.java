package com.icaroerasmo.config;

import com.icaroerasmo.properties.RtspProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class Beans {
    @Bean
    public ExecutorService executorService(RtspProperties rtspProperties) {
        return Executors.newFixedThreadPool(rtspProperties.getCameras().size()*3);
    }
}
