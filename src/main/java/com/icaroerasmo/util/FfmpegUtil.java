package com.icaroerasmo.util;

import com.icaroerasmo.properties.JavaRtspProperties;
import com.icaroerasmo.properties.StorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.stream.Stream;

@Log4j2
@Component
@RequiredArgsConstructor
public class FfmpegUtil {

    private final JavaRtspProperties javaRtspProperties;
    private final ReentrantLock indexFileLock = new ReentrantLock();
    private final PropertiesUtil propertiesUtil;

    public Map<String, String> extractInfoFromFileName(String input) {

        Map<String, String> dateMap = new HashMap<>();

        String regex = "(.+)(\\d{4})-(\\d{2})-(\\d{2})_(\\d{2})-\\d{2}-\\d{2}\\.mkv";
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

    private void deleteFilesToSaveSpace(Long saveUpTo) {
        log.info("Deleting files");

        final StorageProperties storageProperties = javaRtspProperties.getStorageProperties();
        final Path recordsFolder = Paths.get(storageProperties.getRecordsFolder());
        final Path indexFile = recordsFolder.resolve(".index");

        try {
            long sum = 0l;

            List<String> fileList = Files.readAllLines(indexFile);

            List<Path> filesToDelete = new ArrayList<>();

            int index = 0;

            while(sum <= saveUpTo && index < fileList.size()) {

                if(fileList.isEmpty()) {
                    break;
                }

                final String fileRecord = fileList.get(index++);

                String[] file = fileRecord.split(",");
                Path filePath = Paths.get(file[0]);
                sum += Long.parseLong(file[1]);

                filesToDelete.add(filePath);
            }

            filesToDelete.stream().parallel().forEach(file -> {
                try {
                    Files.deleteIfExists(file);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            Files.write(indexFile, fileList.subList(index, fileList.size()));

        } catch (Exception e) {
            log.error("Error deleting files: {}", e.getMessage());
            log.debug("Error deleting files: {}", e.getMessage(), e);
        }
    }

    public void deleteEmptyFolders(Path files) {
        log.info("Deleting empty folders");
        deleteEmptyFoldersRecursively(Stream.of(files));
        log.info("Done deleting empty folders");
    }

    private void deleteEmptyFoldersRecursively(Stream<Path> folders) {

        try {
            folders.filter(Files::isDirectory).forEach(directory -> {
                try {
                    if(Files.list(directory).findAny().isEmpty()) {
                        log.info("Deleting folder: {}", directory);
                        Files.delete(directory);
                    } else {
                        deleteEmptyFoldersRecursively(Files.list(directory));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            log.error("Error deleting folder: {}", e.getMessage());
        }
    }

    public void moveFilesToRecordsFolder(List<String> fileNames) {

        log.info("Moving files to records folder");

        final StorageProperties storageProperties = javaRtspProperties.getStorageProperties();
        final Path tmpFolder = Paths.get(storageProperties.getTmpFolder());
        final Path recordsFolder = Paths.get(storageProperties.getRecordsFolder());
        final Path indexFile = recordsFolder.resolve(".index");

        fileNames.parallelStream().
            map(fileName -> Map.entry(fileName, tmpFolder.resolve(fileName))).
            filter(entry -> Files.exists(entry.getValue())).
            map(entry -> {
                final String fileName = entry.getKey();
                final Path originPath = entry.getValue();
                final Map<String, String> dateMap = extractInfoFromFileName(fileName);
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
            forEach(
                entry -> {

                    final Path originPath = entry.getKey();
                    final Path destinationPath = entry.getValue();

                    try {

                        log.info("Moving file: {} to {}", entry.getKey(), entry.getValue());

                        long maxFolderSizeInBytes =
                                propertiesUtil.storageUnitConverter(
                                        storageProperties.getMaxRecordsFolderSize(), "B");

                        long sizeOfFolder = propertiesUtil.sizeOfFile(recordsFolder);
                        long sizeOfFile = propertiesUtil.sizeOfFile(originPath);
                        long probableSize = sizeOfFolder + sizeOfFile;

                        if(probableSize > maxFolderSizeInBytes) {
                            deleteFilesToSaveSpace(probableSize-maxFolderSizeInBytes);
                        }

                        Files.move(originPath, destinationPath);

                        BasicFileAttributes attrs = Files.readAttributes(destinationPath, BasicFileAttributes.class);

                        String csvLine = String.format("%s,%d,%s%n", destinationPath, attrs.size(), attrs.lastModifiedTime());
                        log.info("Writing to index file: {}", csvLine.substring(0, csvLine.length()-1));

                        indexFileLock.lock();

                        Files.write(indexFile, csvLine.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

                    } catch (Exception e) {
                        log.error("Error moving file: {}", e.getMessage(), e);
                        log.debug("Error moving file: {}", e.getMessage());
                        return;
                    } finally {
                        indexFileLock.unlock();
                    }

                    log.info("File {} moved successfully.", entry.getKey());
                }
            );

        log.info("Done moving files to records folder");
    }
}