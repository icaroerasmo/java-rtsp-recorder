package com.icaroerasmo.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "rtsp")
@PropertySource("classpath:/config.yaml")
public class RtspProperties {

    private String timeout;
    private List<Camera> cameras;

    @Data
    public static class Camera {
        private String url;
        private String name;
        private String host;
        private String port;
        private String format;
        private String username;
        private String password;
    }
}
