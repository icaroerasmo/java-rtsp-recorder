package com.icaroerasmo.util;

import com.icaroerasmo.properties.StorageProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Log4j2
@Component
@RequiredArgsConstructor
public class FfmpegUtil {

    public static final String INDEX = ".index";

    @Getter
    private final ReentrantLock indexFileLock = new ReentrantLock();

    private final StorageProperties storageProperties;
    private final PropertiesUtil propertiesUtil;

    public Map<String, String> extractInfoFromFileName(String input) {

        Map<String, String> dateMap = new HashMap<>();

        String regex = "(.+)(\\d{4})-(\\d{2})-(\\d{2})_(\\d{2})-(\\d{2})-(\\d{2})\\.mkv";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        if(!matcher.matches()) {
            throw new IllegalArgumentException("Input string does not match the expected pattern.");
        }

        final String camName = matcher.group(1);
        final int year = Integer.parseInt(matcher.group(2));
        final int month = Integer.parseInt(matcher.group(3));
        final int day = Integer.parseInt(matcher.group(4));
        final int hour = Integer.parseInt(matcher.group(5));
        final int minute = Integer.parseInt(matcher.group(6));
        final int second = Integer.parseInt(matcher.group(7));

        dateMap.put("camName", camName);
        dateMap.put("year", String.valueOf(year));
        dateMap.put("month", String.valueOf(month));
        dateMap.put("day", String.valueOf(day));
        dateMap.put("hour", String.valueOf(hour));
        dateMap.put("minute", String.valueOf(minute));
        dateMap.put("second", String.valueOf(second));

        return dateMap;
    }

    private void deleteFilesFromIndex(Long saveUpTo) {
        log.info("Deleting files from index");

        final Path recordsFolder = Paths.get(storageProperties.getRecordsFolder());
        final Path indexFile = recordsFolder.resolve(INDEX);

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

            if(indexFile.toFile().exists()) {
                Files.copy(indexFile, tmpFile, StandardCopyOption.REPLACE_EXISTING);
            }

            // Write new file list to tmp file
            Files.write(tmpFile, fileList.subList(index, fileList.size()));

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
                try(Stream<Path> files = Files.list(directory)) {
                    if(files.findAny().isEmpty()) {
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

        final Path tmpFolder = Paths.get(storageProperties.getTmpFolder());
        final Path recordsFolder = Paths.get(storageProperties.getRecordsFolder());
        final Path indexFile = recordsFolder.resolve(INDEX);

        fileNames.parallelStream().
            map(fileName -> Map.entry(fileName, tmpFolder.resolve(fileName))).
            filter(entry -> Files.exists(entry.getValue())).
            sorted(filesNamesComparator()).
            map(filesPathMapper(recordsFolder)).
            forEachOrdered(filesMover(recordsFolder, indexFile));

        log.info("Done moving files to records folder");
    }

    private Consumer<Map.Entry<Path, Path>> filesMover(Path recordsFolder, Path indexFile) {
        return (entry) -> {

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

            String indexLine;

            try {
                Files.move(originPath, destinationPath);
                BasicFileAttributes attrs = Files.readAttributes(destinationPath, BasicFileAttributes.class);
                indexLine = String.format("%s,%d,%s%n", destinationPath, attrs.size(), attrs.lastModifiedTime());
            } catch (IOException e) {
                log.error("Error moving file: {}", e.getMessage(), e);
                log.debug("Error moving file: {}", e.getMessage());
                return;
            }

            boolean success = true;

            try {
                log.info("Writing to index file: {}", indexLine.substring(0, indexLine.length()-1));

                indexFileLock.lock();

                // Creates copy of original index file
                final Path tmpFile = generateTmpPath(indexFile);

                if(indexFile.toFile().exists()) {
                    Files.copy(indexFile, tmpFile, StandardCopyOption.REPLACE_EXISTING);
                }

                // Write new file name to index file
                Files.write(tmpFile, indexLine.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

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
        };
    }

    private Function<Map.Entry<String, Path>, Map.Entry<Path, Path>> filesPathMapper(Path recordsFolder) {
        return entry -> {
            final String fileName = entry.getKey();
            final Path originPath = entry.getValue();
            final Map<String, String> dateMap = extractInfoFromFileName(fileName);

            final int month = Integer.parseInt(dateMap.get("month"));
            final String monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.getDefault());

            final Path destinationFolder =
                    Paths.get(recordsFolder.toString(), dateMap.get("year"),
                            monthName, dateMap.get("day"),
                            dateMap.get("hour"), dateMap.get("camName"));
            final Path destinationPath = destinationFolder.resolve(fileName);

            if (!Files.exists(destinationFolder)) {
                try {
                    Files.createDirectories(destinationFolder);
                } catch (Exception e) {
                    log.error("Error creating folder: {}", e.getMessage());
                    log.debug("Error creating folder: {}", e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            }
            return Map.entry(originPath, destinationPath);
        };
    }

    private Comparator<Map.Entry<String, Path>> filesNamesComparator() {
        return (entry1, entry2) -> {

            final Map<String, String> info1 = extractInfoFromFileName(entry1.getKey());
            final Map<String, String> info2 = extractInfoFromFileName(entry2.getKey());

            // Multiplies by -1 to reverse the order
            BiFunction<Integer, Integer, Integer> intComparator = (Integer x, Integer y) -> -1 * Integer.compare(x, y);

            int year1 = Integer.parseInt(info1.get("year"));
            int year2 = Integer.parseInt(info2.get("year"));

            if (year1 != year2) {
                return intComparator.apply(year1, year2);
            }

            int month1 = Integer.parseInt(info1.get("month"));
            int month2 = Integer.parseInt(info2.get("month"));

            if (month1 != month2) {
                return intComparator.apply(month1, month2);
            }

            int day1 = Integer.parseInt(info1.get("day"));
            int day2 = Integer.parseInt(info2.get("day"));

            if (day1 != day2) {
                return intComparator.apply(day1, day2);
            }

            int hour1 = Integer.parseInt(info1.get("hour"));
            int hour2 = Integer.parseInt(info2.get("hour"));

            if (hour1 != hour2) {
                return intComparator.apply(hour1, hour2);
            }

            int minute1 = Integer.parseInt(info1.get("minute"));
            int minute2 = Integer.parseInt(info2.get("minute"));

            if (minute1 != minute2) {
                return intComparator.apply(minute1, minute2);
            }

            int second1 = Integer.parseInt(info1.get("second"));
            int second2 = Integer.parseInt(info2.get("second"));

            if (second1 != second2) {
                return intComparator.apply(second1, second2);
            }

            return entry1.getKey().compareTo(entry2.getKey());
        };
    }

    private Path generateTmpPath(Path originalFile) {
        return originalFile.getParent().
                resolve(originalFile.getName(
                        originalFile.getNameCount() - 1) + ".tmp");
    }
}