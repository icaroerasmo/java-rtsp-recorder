package com.icaroerasmo.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "rtsp")
public class Rtsp implements ConfigYaml {

    private List<Camera> cameras;

    private static class Camera {
        private String name;
        private String host;
        private String port;
        private String protocol;
        private String username;
        private String password;
    }
}
