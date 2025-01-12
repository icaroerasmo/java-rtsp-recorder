package com.icaroerasmo.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Data
@Configuration
@ConfigurationProperties(prefix = "storage")
public class StorageProperties implements ConfigYaml {
    private String videoDuration;
    private String fileMoverSleep;
    private String tmpFolder;
    private String recordsFolder;
    private String maxRecordsSize;
}