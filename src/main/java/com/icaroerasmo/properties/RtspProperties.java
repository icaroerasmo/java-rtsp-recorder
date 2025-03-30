package com.icaroerasmo.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "rtsp")
public class RtspProperties implements ConfigYaml {

    private String timeout = "5s";
    private String videoDuration = "5m";
    private List<Camera> cameras;

    @Data
    public static class Camera {
        private String url;
        private String name;
        private String host;
        private TransportProtocol protocol = TransportProtocol.TCP;
        private String port;
        private String format;
        private String username;
        private String password;
    }

    public enum TransportProtocol {
        TCP, UDP
    }
}
