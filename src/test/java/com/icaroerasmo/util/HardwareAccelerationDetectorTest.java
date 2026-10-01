package com.icaroerasmo.util;

import com.icaroerasmo.properties.RtspProperties.HardwareAcceleration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HardwareAccelerationDetectorTest {

    @TempDir
    Path tempDir;

    @Test
    void resolvesNullAsNone() {
        assertEquals(HardwareAcceleration.NONE,
                HardwareAccelerationDetector.resolve(null, null));
    }

    @Test
    void resolvesNonAutoValueAsIs() {
        assertEquals(HardwareAcceleration.NVIDIA,
                HardwareAccelerationDetector.resolve(HardwareAcceleration.NVIDIA, null));
        assertEquals(HardwareAcceleration.RADEON,
                HardwareAccelerationDetector.resolve(HardwareAcceleration.RADEON, null));
        assertEquals(HardwareAcceleration.CPU,
                HardwareAccelerationDetector.resolve(HardwareAcceleration.CPU, null));
    }

    @Test
    void autoDetectsNvidiaWhenDevicePresent() throws Exception {
        Path nvidia = tempDir.resolve("nvidia0");
        Files.createFile(nvidia);

        assertEquals(HardwareAcceleration.NVIDIA,
                HardwareAccelerationDetector.detect(nvidia.toString(), tempDir.resolve("renderD128").toString()));
    }

    @Test
    void autoDetectsVaapiWhenOnlyDriPresent() throws Exception {
        Path dri = tempDir.resolve("renderD128");
        Files.createFile(dri);

        assertEquals(HardwareAcceleration.RADEON,
                HardwareAccelerationDetector.detect(tempDir.resolve("nvidia0").toString(), dri.toString()));
    }

    @Test
    void autoFallsBackToCpuWhenNoDevice() {
        assertEquals(HardwareAcceleration.CPU,
                HardwareAccelerationDetector.detect(
                        tempDir.resolve("nvidia0").toString(),
                        tempDir.resolve("renderD128").toString()));
    }
}
