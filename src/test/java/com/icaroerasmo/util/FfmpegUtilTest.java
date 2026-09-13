package com.icaroerasmo.util;

import com.icaroerasmo.properties.StorageProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FfmpegUtilTest {

    // extractInfoFromFileName and deleteEmptyFolders do not touch storage properties.
    private final FfmpegUtil ffmpegUtil = new FfmpegUtil(null, null);

    @Test
    void extractsInfoFromFileName() {
        Map<String, String> info = ffmpegUtil.extractInfoFromFileName("cam1_2024-05-20_14-30-10.mkv");

        assertEquals("cam1_", info.get("camName"));
        assertEquals("2024", info.get("year"));
        assertEquals("5", info.get("month"));
        assertEquals("20", info.get("day"));
        assertEquals("14", info.get("hour"));
        assertEquals("30", info.get("minute"));
        assertEquals("10", info.get("second"));
        assertEquals(7, info.size());
    }

    @Test
    void extractsCamNameContainingUnderscores() {
        Map<String, String> info = ffmpegUtil.extractInfoFromFileName("garage_back_2024-01-01_00-00-00.mkv");
        assertEquals("garage_back_", info.get("camName"));
        assertEquals("2024", info.get("year"));
        assertEquals("1", info.get("month"));
    }

    @Test
    void throwsOnNonMatchingFileName() {
        assertThrows(IllegalArgumentException.class,
                () -> ffmpegUtil.extractInfoFromFileName("cam1.mkv"));
        assertThrows(IllegalArgumentException.class,
                () -> ffmpegUtil.extractInfoFromFileName("cam1_2024-05-20_14-30-10.mp4"));
        assertThrows(IllegalArgumentException.class,
                () -> ffmpegUtil.extractInfoFromFileName(""));
    }

    @Test
    void deletesTopLevelEmptyFolder(@TempDir Path tempDir) throws IOException {
        Path emptyDir = Files.createDirectory(tempDir.resolve("empty"));
        Path file = Files.write(tempDir.resolve("keep.txt"), new byte[]{1});

        ffmpegUtil.deleteEmptyFolders(tempDir);

        assertFalse(Files.exists(emptyDir));
        assertTrue(Files.exists(file));
    }

    @Test
    void deletesInnermostEmptyFoldersInSinglePass(@TempDir Path tempDir) throws IOException {
        Path a = Files.createDirectories(tempDir.resolve("a/b/c"));

        ffmpegUtil.deleteEmptyFolders(tempDir);

        // Only the innermost empty folder is removed in one pass.
        assertFalse(Files.exists(a));
        assertTrue(Files.exists(tempDir.resolve("a/b")));

        // A second pass removes the now-empty parent.
        ffmpegUtil.deleteEmptyFolders(tempDir);
        assertFalse(Files.exists(tempDir.resolve("a/b")));
    }

    @Test
    void keepsFoldersContainingFiles(@TempDir Path tempDir) throws IOException {
        Path dirWithFile = Files.createDirectories(tempDir.resolve("hasfiles"));
        Files.write(dirWithFile.resolve("segment.mkv"), new byte[]{1, 2, 3});

        ffmpegUtil.deleteEmptyFolders(tempDir);

        assertTrue(Files.exists(dirWithFile));
    }

    @Test
    void doesNotDeleteExistingRecordsWhenFolderExceedsMaxSize(@TempDir Path tempDir) throws IOException {
        Path tmp = Files.createDirectories(tempDir.resolve("tmp"));
        Path records = Files.createDirectories(tempDir.resolve("records"));

        Path preExisting = Files.write(records.resolve("old_file.mkv"), new byte[4096]);
        Files.write(tmp.resolve("cam1_2024-05-20_14-30-10.mkv"), new byte[10]);

        StorageProperties storageProperties = new StorageProperties();
        storageProperties.setTmpFolder(tmp.toString());
        storageProperties.setRecordsFolder(records.toString());
        storageProperties.setMaxRecordsFolderSize("1KB");

        FfmpegUtil ffmpegUtil = new FfmpegUtil(storageProperties, new PropertiesUtil());
        ffmpegUtil.moveFilesToRecordsFolder(List.of("cam1_2024-05-20_14-30-10.mkv"));

        assertTrue(Files.exists(preExisting),
                "Mover must not delete existing records even when folder exceeds the max size");
        assertFalse(Files.exists(tmp.resolve("cam1_2024-05-20_14-30-10.mkv")));
    }
}
