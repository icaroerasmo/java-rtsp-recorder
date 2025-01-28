package com.icaroerasmo.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "rclone")
public class RcloneProperties {
    private String configLocation = "/app/config/rclone.conf";
    private String deleteCron = "0 0 0 * * *";
    private String rmdirsCron = "0 10 0 * * *";
    private String dedupeCron = "0 20 0 * * *";
    private String syncCron = "0 */10 * * * *";
    private String transferMethod = "copy";
    private String destinationFolder;
    private List<String> excludePatterns = new ArrayList<>();
    private boolean ignoreExisting = false;
}
