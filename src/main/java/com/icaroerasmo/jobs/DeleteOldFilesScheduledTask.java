package com.icaroerasmo.jobs;

import com.icaroerasmo.properties.StorageProperties;
import com.icaroerasmo.util.FfmpegUtil;
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
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

import static com.icaroerasmo.util.FfmpegUtil.INDEX;

@Log4j2
@Component
@RequiredArgsConstructor
public class DeleteOldFilesScheduledTask {

    private final FfmpegUtil ffmpegUtil;
    private final PropertiesUtil propertiesUtil;
    private final StorageProperties storageProperties;

    @Scheduled(cron = "#{@storageProperties.deleteOldFilesCron}")
    private void rcloneDedupe() throws IOException {
        final ZonedDateTime lastModified = findOldestModifiedDate();
        log.info("Started deleting files older than {}", lastModified.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
        final Path recordsFolder = Paths.get(storageProperties.getRecordsFolder());
        folderDeleter(lastModified, recordsFolder);
        log.info("Finished deleting files older than {}", lastModified.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
    }

    private void deleteOldFiles(ZonedDateTime lastModified, Path path) throws IOException {
        if(isFolder(path)) {
            folderDeleter(lastModified, path);
            if(isEmptyFolder(path)) {
                log.info("Deleting empty folder: {}", path);
                Files.delete(path);
                log.info("Deleted empty folder: {}", path);
            }
        } else {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            if(isOlderThanOldestModifiedInIndex(attrs, lastModified)) {
                log.info("Deleting old file: {}", path);
                Files.delete(path);
                log.info("Deleted old file: {}", path);
            }
        }
    }

    private void folderDeleter(ZonedDateTime lastModified, Path path) throws IOException {
        try(Stream<Path> files = Files.list(path)) {
            files.forEach(file -> {
                try {
                    deleteOldFiles(lastModified, file);
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

    private boolean isOlderThanOldestModifiedInIndex(BasicFileAttributes attrs, ZonedDateTime lastModified) {

        final ZonedDateTime fileLastModified =
                attrs.lastModifiedTime().toInstant().
                        atZone(ZoneId.systemDefault());

        return fileLastModified.isBefore(lastModified);
    }

    private ZonedDateTime findOldestModifiedDate() throws IOException {

        ReentrantLock lock = ffmpegUtil.getIndexFileLock();

        final Path recordsFolder = Paths.get(storageProperties.getRecordsFolder());
        final Path indexFile = recordsFolder.resolve(INDEX);

        ZonedDateTime lastModified;

        try {
            lock.lock();

            final String fileRecord = Files.readAllLines(indexFile).getFirst();
            String[] fileData = fileRecord.split(",");

            String dateTime = fileData[fileData.length-1];
            lastModified = Instant.parse(dateTime).atZone(ZoneId.systemDefault());

        } catch (Exception e) {
            log.error("Error reading index file: {}", e.getMessage());
            log.debug("Error reading index file: {}", e.getMessage(), e);
            throw e;
        } finally {
            lock.unlock();
        }

        return lastModified;
    }
}
