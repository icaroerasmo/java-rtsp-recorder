package com.icaroerasmo.util;

import com.icaroerasmo.properties.RtspProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropertiesUtilTest {

    private final PropertiesUtil propertiesUtil = new PropertiesUtil();

    // ---------------------------------------------------------------- durationParser

    @Test
    void parsesMinutesToSeconds() {
        assertEquals("300", propertiesUtil.durationParser("5m", TimeUnit.SECONDS));
    }

    @Test
    void parsesSecondsToMilliseconds() {
        assertEquals("5000", propertiesUtil.durationParser("5s", TimeUnit.MILLISECONDS));
    }

    @Test
    void parsesSecondsToMicroseconds() {
        assertEquals("5000000", propertiesUtil.durationParser("5s", TimeUnit.MICROSECONDS));
    }

    @Test
    void parsesCombinedDuration() {
        // 1d 2h 3m 4s 5mm = 93.784.005 ms
        assertEquals("93784005", propertiesUtil.durationParser("1d2h3m4s5mm", TimeUnit.MILLISECONDS));
    }

    @Test
    void parsesMixedDurationToSecondsTruncates() {
        // 1m30s = 90.000 ms -> 90 s
        assertEquals("90", propertiesUtil.durationParser("1m30s", TimeUnit.SECONDS));
    }

    @Test
    void throwsOnInvalidDuration() {
        assertThrows(IllegalArgumentException.class,
                () -> propertiesUtil.durationParser("not-a-duration", TimeUnit.SECONDS));
        assertThrows(IllegalArgumentException.class,
                () -> propertiesUtil.durationParser("500", TimeUnit.SECONDS));
    }

    @Test
    void convertToUnitConvertsMilliseconds() {
        assertEquals(5, propertiesUtil.convertToUnit(TimeUnit.SECONDS, 5000));
        assertEquals(300, propertiesUtil.convertToUnit(TimeUnit.SECONDS, 300000));
    }

    // ---------------------------------------------------------- storageUnitConverter

    @Test
    void convertsStorageUnits() {
        assertEquals(10485760, propertiesUtil.storageUnitConverter("10MB", "B"));
        assertEquals(10240, propertiesUtil.storageUnitConverter("10MB", "KB"));
        assertEquals(1073741824, propertiesUtil.storageUnitConverter("1GB", "B"));
        assertEquals(1, propertiesUtil.storageUnitConverter("1GB", "GB"));
        assertEquals(12800, propertiesUtil.storageUnitConverter("100Kb", "B"));
        assertEquals(100, propertiesUtil.storageUnitConverter("100Kb", "Kb"));
        assertEquals(1024, propertiesUtil.storageUnitConverter("1KB", "B"));
    }

    @Test
    void convertsBitsUnits() {
        // 10 bits -> 1 byte (integer division) -> 8 bits back
        assertEquals(1, propertiesUtil.storageUnitConverter("10b", "B"));
        assertEquals(8, propertiesUtil.storageUnitConverter("10b", "b"));
    }

    @Test
    void throwsOnInvalidStorageValue() {
        assertThrows(IllegalArgumentException.class,
                () -> propertiesUtil.storageUnitConverter("10M", "B"));
        assertThrows(IllegalArgumentException.class,
                () -> propertiesUtil.storageUnitConverter("abc", "B"));
        assertThrows(IllegalArgumentException.class,
                () -> propertiesUtil.storageUnitConverter("10", "B"));
    }

    @Test
    void throwsOnInvalidTargetUnit() {
        assertThrows(IllegalArgumentException.class,
                () -> propertiesUtil.storageUnitConverter("10MB", "X"));
    }

    // -------------------------------------------------------------- cameraUrlParser

    @Test
    void returnsExplicitUrlWhenSet() {
        RtspProperties.Camera camera = new RtspProperties.Camera();
        camera.setUrl("rtsp://custom/stream");

        assertEquals("rtsp://custom/stream", propertiesUtil.cameraUrlParser(camera));
    }

    @Test
    void buildsUrlFromPartsWhenUrlNotSet() {
        RtspProperties.Camera camera = new RtspProperties.Camera();
        camera.setUsername("user");
        camera.setPassword("pass");
        camera.setHost("192.168.0.10");
        camera.setPort("8554");
        camera.setFormat("live");

        assertEquals("rtsp://user:pass@192.168.0.10:8554/live",
                propertiesUtil.cameraUrlParser(camera));
    }

    // ------------------------------------------------------------------- sizeOfFile

    @Test
    void sizeOfSingleFile(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("segment.mkv");
        Files.write(file, new byte[150]);

        assertEquals(150, propertiesUtil.sizeOfFile(file));
        assertEquals(150, propertiesUtil.sizeOfFile(file.toFile()));
    }

    @Test
    void sizeOfDirectoryCountsNestedFiles(@TempDir Path tempDir) throws IOException {
        Path subDir = Files.createDirectories(tempDir.resolve("a/b"));
        Files.write(tempDir.resolve("f1.mkv"), new byte[100]);
        Files.write(subDir.resolve("f2.mkv"), new byte[50]);

        long total = propertiesUtil.sizeOfFile(tempDir);

        // Sum of file contents must be included; directory entries may add platform
        // specific directory sizes on top.
        assertTrue(total >= 150);
    }

    // ---------------------------------------------------------------- @TempDir note
    // No test launches ffmpeg/rclone, touches RabbitMQ or the network.
}
