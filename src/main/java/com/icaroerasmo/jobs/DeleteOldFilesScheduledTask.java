package com.icaroerasmo.jobs;

import com.icaroerasmo.properties.StorageProperties;
import com.icaroerasmo.util.PropertiesUtil;
import com.icaroerasmo.util.Utilities;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Log4j2
@Component
@RequiredArgsConstructor
public class DeleteOldFilesScheduledTask {

    private final PropertiesUtil propertiesUtil;
    private final StorageProperties storageProperties;
    private final Utilities utilities;

    @Scheduled(cron = "#{@storageProperties.deleteOldFilesCron}")
    private void rcloneDedupe() throws IOException {
        log.info("Started deleting files older than {}", utilities.getFullTimeAmount(getTimeInMillis()));
        final Path recordsFolder = Paths.get(storageProperties.getRecordsFolder());
        folderDeleter(recordsFolder);
        log.info("Finished deleting files older than {}", utilities.getFullTimeAmount(getTimeInMillis()));
    }

    private void deleteOldFiles(Path path) throws IOException {
        if(isFolder(path)) {
            folderDeleter(path);
            if(isEmptyFolder(path)) {
                log.info("Deleting empty folder: {}", path);
                Files.delete(path);
                log.info("Deleted empty folder: {}", path);
            }
        } else {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            if(isOlderThanMaxAge(attrs)) {
                log.info("Deleting old file: {}", path);
                Files.delete(path);
                log.info("Deleted old file: {}", path);
            }
        }
    }

    private void folderDeleter(Path path) throws IOException {
        try(Stream<Path> files = Files.list(path)) {
            files.forEach(file -> {
                try {
                    deleteOldFiles(file);
                } catch (IOException e) {
                    log.error("Old files Deletion job: Error deleting file: {}", file);
                }
            });
        }
    }

    private static boolean isFolder(Path path) {
        return path.toFile().isDirectory();
    }

    private static boolean isEmptyFolder(Path path) {
        return isFolder(path) && Objects.requireNonNull(path.toFile().list()).length == 0;
    }

    private boolean isOlderThanMaxAge(BasicFileAttributes attrs) {
        return Objects.nonNull(attrs) &&
                attrs.creationTime().toMillis() <
                        System.currentTimeMillis() - getTimeInMillis();
    }

    private Long getTimeInMillis() {
        return Long.parseLong(propertiesUtil.durationParser(
                storageProperties.getMaxAgeVideoFiles(), TimeUnit.MILLISECONDS));
    }
}
