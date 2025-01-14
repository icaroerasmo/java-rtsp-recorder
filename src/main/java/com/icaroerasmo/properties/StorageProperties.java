package com.icaroerasmo.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "storage")
public class StorageProperties implements ConfigYaml {
    private String fileMoverSleep = "1m";
    private String tmpFolder = "/app/tmp";
    private String recordsFolder = "/app/records";
    private String maxRecordsFolderSize = "10G";
}