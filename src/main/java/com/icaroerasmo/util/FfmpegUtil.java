package com.icaroerasmo.util;

import com.icaroerasmo.properties.JavaRtspProperties;
import com.icaroerasmo.properties.StorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
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

    private final ReentrantLock indexFileLock = new ReentrantLock();

    private final JavaRtspProperties javaRtspProperties;
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
            String monthName = date.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault());

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

    private void deleteFilesFromIndex(Long saveUpTo) {
        log.info("Deleting files from index");

        final StorageProperties storageProperties = javaRtspProperties.getStorageProperties();
        final Path recordsFolder = Paths.get(storageProperties.getRecordsFolder());
        final Path indexFile = recordsFolder.resolve(".index");

        List<String> fileList;

        try {
            indexFileLock.lock();
            fileList = Files.readAllLines(indexFile);
        } catch(Exception e) {
            log.error("Error reading index file: {}", e.getMessage());
            log.debug("Error reading index file: {}", e.getMessage(), e);
            indexFileLock.unlock();
            return;
        }

        if(fileList.isEmpty()) {
            log.info("Index file is empty");
            indexFileLock.unlock();
            return;
        }

        int index = 0;
        long sum = 0;

        List<Path> filesToDelete = new ArrayList<>();

        while(sum <= saveUpTo && index < fileList.size()) {

            final String fileRecord = fileList.get(index++);

            try {
                log.info("Capturing data from index to be deleted: {}", fileRecord);
                String[] fileData = fileRecord.split(",");
                Path filePath = Paths.get(fileData[0]);
                sum += Long.parseLong(fileData[1]);
                filesToDelete.add(filePath);
                log.info("File deleted from index successfully: {}", filePath);
            } catch (Exception e) {
                log.error("Error capturing file from index to be deleted: {}. Line: {}", e.getMessage(), fileRecord);
                log.debug("Error capturing file from index to be deleted: {}. Line: {}", e.getMessage(), fileRecord, e);
            }
        }

        filesToDelete.stream().parallel().forEach(file -> {
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                log.error("Error deleting file from index: {}", e.getMessage());
                log.debug("Error deleting file from index: {}", e.getMessage(), e);
            }
        });

        try {

            // Creates tmp file
            final Path tmpFile = generateTmpPath(indexFile);

            // Write new file list to tmp file
            Files.write(tmpFile, fileList.subList(index, fileList.size()), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            // Replace original index file with new one
            Files.copy(tmpFile, indexFile, StandardCopyOption.REPLACE_EXISTING);

        } catch (Exception e) {
            log.error("Error rewriting files to index: {}", e.getMessage());
            log.debug("Error rewriting files to index: {}", e.getMessage(), e);
        }

        indexFileLock.unlock();

        log.info("Done deleting files from index");
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
                                dateMap.get("month"), dateMap.get("day"),
                                dateMap.get("hour"), dateMap.get("camName"));
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

                    log.info("Moving file: {} to {}", entry.getKey(), entry.getValue());

                    long maxFolderSizeInBytes =
                            propertiesUtil.storageUnitConverter(
                                    storageProperties.getMaxRecordsFolderSize(), "B");

                    long sizeOfFolder = propertiesUtil.sizeOfFile(recordsFolder);
                    long sizeOfFile = propertiesUtil.sizeOfFile(originPath);
                    long probableSize = sizeOfFolder + sizeOfFile;

                    if(probableSize > maxFolderSizeInBytes) {
                        deleteFilesFromIndex(probableSize-maxFolderSizeInBytes);
                    }

                    String csvLine = "";

                    try {
                        Files.move(originPath, destinationPath);
                        BasicFileAttributes attrs = Files.readAttributes(destinationPath, BasicFileAttributes.class);
                        csvLine = String.format("%s,%d,%s%n", destinationPath, attrs.size(), attrs.lastModifiedTime());
                    } catch (IOException e) {
                        log.error("Error moving file: {}", e.getMessage(), e);
                        log.debug("Error moving file: {}", e.getMessage());
                        return;
                    }

                    boolean success = true;

                    try {
                        log.info("Writing to index file: {}", csvLine.substring(0, csvLine.length()-1));

                        indexFileLock.lock();

                        // Creates copy of orifinal index file
                        final Path tmpFile = generateTmpPath(indexFile);

                        if(indexFile.toFile().exists()) {
                            Files.copy(indexFile, tmpFile, StandardCopyOption.REPLACE_EXISTING);
                        }

                        // Write new file name to index file
                        Files.write(tmpFile, csvLine.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

                        // Replace original index file with new one
                        Files.copy(tmpFile, indexFile, StandardCopyOption.REPLACE_EXISTING);

                    } catch (Exception e) {
                        log.error("Error writing to index file: {}", e.getMessage(), e);
                        log.debug("Error writing to index file: {}", e.getMessage());
                        success = false;
                    } finally {
                        indexFileLock.unlock();
                    }

                    if(success) {
                        log.info("File {} moved successfully.", entry.getKey());
                    } else {
                        log.error("Error when trying to move file: {}.", entry.getKey());
                    }
                }
            );

        log.info("Done moving files to records folder");
    }

    private static Path generateTmpPath(Path originalFile) {
        return originalFile.getParent().
                resolve(originalFile.getName(
                        originalFile.getNameCount() - 1) + ".tmp");
    }
}