package com.icaroerasmo.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "storage")
public class StorageProperties implements ConfigYaml {
    private String fileMoverInterval = "5m";
    private String deleteOldFilesCron = "0 30 0 * * *";
    private String tmpFolder = "/app/data/tmp";
    private String recordsFolder = "/app/data/records";
    private String maxRecordsFolderSize = "10G";
    private String maxAgeRemoteVideoFiles = "20d";
    private String maxAgeLocalVideoFiles = "3d";
}