package com.icaroerasmo.jobs;

import com.icaroerasmo.properties.JavaRtspProperties;
import com.icaroerasmo.properties.RcloneProperties;
import com.icaroerasmo.properties.RtspProperties;
import com.icaroerasmo.properties.StorageProperties;
import com.icaroerasmo.util.FfmpegUtil;
import com.icaroerasmo.util.PropertiesUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileMoverScheduledTaskTest {

    // The camName captured from the filename regex includes the trailing underscore.
    private static final String SEGMENT_FILE = "camA_2024-05-20_14-30-10.mkv";

    @Test
    void movesFileOfHealthyCameraWhenAnotherCameraHasNoSegmentsFile(@TempDir Path tempDir) throws IOException {
        Path tmpFolder = Files.createDirectories(tempDir.resolve("tmp"));
        Path recordsFolder = Files.createDirectories(tempDir.resolve("records"));

        Files.write(tmpFolder.resolve(SEGMENT_FILE), new byte[]{1});
        Files.write(tmpFolder.resolve(".camA_done_segments"), List.of(SEGMENT_FILE));

        FileMoverScheduledTask task = moverTask(tmpFolder, recordsFolder, "camA", "camB");

        task.filesMover();

        assertFalse(Files.exists(tmpFolder.resolve(SEGMENT_FILE)));
        assertTrue(fileExistsUnder(recordsFolder, SEGMENT_FILE));
    }

    @Test
    void skippedCameraWithMissingSegmentsFileDoesNotAbortOthers(@TempDir Path tempDir) throws IOException {
        Path tmpFolder = Files.createDirectories(tempDir.resolve("tmp"));
        Path recordsFolder = Files.createDirectories(tempDir.resolve("records"));

        Files.write(tmpFolder.resolve(SEGMENT_FILE), new byte[]{1});
        Files.write(tmpFolder.resolve(".camB_done_segments"), List.of(SEGMENT_FILE));

        FileMoverScheduledTask task = moverTask(tmpFolder, recordsFolder, "camA", "camB");

        task.filesMover();

        // camA (missing segment file, e.g. offline since boot) must not block camB.
        assertFalse(Files.exists(tmpFolder.resolve(SEGMENT_FILE)));
        assertTrue(fileExistsUnder(recordsFolder, SEGMENT_FILE));
    }

    @Test
    void emptiesSegmentsFileIsNoOp(@TempDir Path tempDir) throws IOException {
        Path tmpFolder = Files.createDirectories(tempDir.resolve("tmp"));
        Path recordsFolder = Files.createDirectories(tempDir.resolve("records"));

        Files.write(tmpFolder.resolve(".camA_done_segments"), new byte[0]);

        FileMoverScheduledTask task = moverTask(tmpFolder, recordsFolder, "camA");

        task.filesMover();
    }

    @Test
    void corruptSegmentListEntryIsIsolatedToItsCamera(@TempDir Path tempDir) throws IOException {
        Path tmpFolder = Files.createDirectories(tempDir.resolve("tmp"));
        Path recordsFolder = Files.createDirectories(tempDir.resolve("records"));

        // This name exists on disk (so it passes the existence filter) but does not match
        // the expected .mkv naming pattern, making FfmpegUtil.extractInfoFromFileName throw.
        String corrupt = "bogus_not_a_date.mkv";
        String camAValid = "camA_2024-05-20_14-30-10.mkv";
        String camBValid = "camB_2024-05-20_15-45-00.mkv";

        Files.write(tmpFolder.resolve(corrupt), new byte[]{1});
        Files.write(tmpFolder.resolve(camAValid), new byte[]{1});
        Files.write(tmpFolder.resolve(camBValid), new byte[]{1});
        Files.write(tmpFolder.resolve(".camA_done_segments"), List.of(corrupt, camAValid));
        Files.write(tmpFolder.resolve(".camB_done_segments"), List.of(camBValid));

        FileMoverScheduledTask task = moverTask(tmpFolder, recordsFolder, "camA", "camB");

        // Must not propagate camA's corrupt entry to the other cameras.
        task.filesMover();

        assertFalse(Files.exists(tmpFolder.resolve(camBValid)));
        assertTrue(fileExistsUnder(recordsFolder, camBValid));
    }

    private static FileMoverScheduledTask moverTask(Path tmpFolder, Path recordsFolder, String... cameraNames) {
        StorageProperties storageProperties = new StorageProperties();
        storageProperties.setTmpFolder(tmpFolder.toString());
        storageProperties.setRecordsFolder(recordsFolder.toString());
        storageProperties.setMaxRecordsFolderSize("10GB");

        RtspProperties rtspProperties = new RtspProperties();
        rtspProperties.setCameras(Stream.of(cameraNames).map(name -> {
            RtspProperties.Camera camera = new RtspProperties.Camera();
            camera.setName(name);
            return camera;
        }).toList());

        JavaRtspProperties javaRtspProperties =
                new JavaRtspProperties(rtspProperties, storageProperties, new RcloneProperties());
        FfmpegUtil ffmpegUtil = new FfmpegUtil(storageProperties, new PropertiesUtil());

        return new FileMoverScheduledTask(ffmpegUtil, javaRtspProperties);
    }

    private static boolean fileExistsUnder(Path root, String fileName) throws IOException {
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.anyMatch(path -> path.getFileName().toString().equals(fileName));
        }
    }
}