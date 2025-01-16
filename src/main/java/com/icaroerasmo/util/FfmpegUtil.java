package com.icaroerasmo.util;

import com.icaroerasmo.properties.JavaRtspProperties;
import com.icaroerasmo.properties.StorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

@Log4j2
@Component
@RequiredArgsConstructor
public class FfmpegUtil {

    private final JavaRtspProperties javaRtspProperties;
    private final ReentrantLock lock = new ReentrantLock();

    public Map<String, String> extractInfoFromFileName(String input) {

        Map<String, String> dateMap = new HashMap<>();

        String regex = ".*?(.+)(\\d{4})-(\\d{2})-(\\d{2})_(\\d{2})-\\d{2}-\\d{2}\\.mkv";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        if (matcher.matches()) {
            String camName = matcher.group(1);
            int year = Integer.parseInt(matcher.group(2));
            int month = Integer.parseInt(matcher.group(3));
            int day = Integer.parseInt(matcher.group(4));
            int hour = Integer.parseInt(matcher.group(5));

            LocalDate date = LocalDate.of(year, month, day);
            String monthName = date.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);

            dateMap.put("camName", camName);
            dateMap.put("year", String.valueOf(year));
            dateMap.put("month", monthName);
            dateMap.put("day", String.valueOf(day));
            dateMap.put("hour", String.valueOf(hour));
        } else {
            throw new IllegalArgumentException("Input string does not match the expected pattern.");
        }

        return dateMap;
    }

    public void moveFilesToRecordsFolder(List<String> fileNames) {

        log.info("Moving files to records folder");

        final StorageProperties storageProperties = javaRtspProperties.getStorageProperties();
        final Path tmpFolder = Paths.get(storageProperties.getTmpFolder());
        final Path recordsFolder = Paths.get(storageProperties.getRecordsFolder());
        final Path indexFile = recordsFolder.resolve(".index");

        fileNames.parallelStream().map(fileName -> {
            Map<String, String> dateMap = extractInfoFromFileName(fileName);
            final Path originPath = tmpFolder.resolve(fileName);
            final Path destinationFolder =
                    Paths.get(recordsFolder.toString(), dateMap.get("year"),
                            dateMap.get("month"), dateMap.get("day"), dateMap.get("hour"), dateMap.get("camName"));
            final Path destinationPath = destinationFolder.resolve(fileName);

            if(!Files.exists(destinationFolder)) {
                try {
                    Files.createDirectories(destinationFolder);
                } catch (Exception e) {
                    log.error("Error creating folder: {}", e.getMessage());
                    log.debug("Error creating folder: {}", e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            }
            return Map.entry(originPath, destinationPath);
        }).
        filter(entry -> Files.exists(entry.getKey())).
        forEach(
            entry -> {

                final Path originPath = entry.getKey();
                final Path destinationPath = entry.getValue();

                try {
                    log.info("Moving file: {} to {}", entry.getKey(), entry.getValue());
                    Files.move(originPath, destinationPath);

                    BasicFileAttributes attrs = Files.readAttributes(destinationPath, BasicFileAttributes.class);
                    String csvLine = String.format("%s,%d,%s%n", destinationPath, attrs.size(), attrs.lastModifiedTime());

                    lock.lock();

                    log.info("Writing to index file: {}", csvLine);
                    Files.write(indexFile, csvLine.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

                } catch (Exception e) {
                    log.error("Error moving file: {}", e.getMessage(), e);
                    log.debug("Error moving file: {}", e.getMessage());
                    return;
                } finally {
                    lock.unlock();
                }

                log.info("File {} moved successfully.", entry.getKey());
            }
        );

        log.info("Done moving files to records folder");
    }
}