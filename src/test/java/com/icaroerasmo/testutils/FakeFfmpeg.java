package com.icaroerasmo.testutils;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;

/**
 * Installs the fake ffmpeg script (test resource {@code fakebin/fake-ffmpeg}) into a
 * folder and writes the control files that drive its behavior. Control files are placed
 * in the camera's tmp folder because {@code ProcessBuilder} inherits the JVM env, so
 * per-process env vars cannot be injected from tests.
 */
public final class FakeFfmpeg {

    private FakeFfmpeg() {
    }

    public static Path install(Path folder) throws IOException {
        Path binary = folder.resolve("fake-ffmpeg");
        try (InputStream in = new ClassPathResource("fakebin/fake-ffmpeg").getInputStream()) {
            Files.copy(in, binary);
        }
        Files.setPosixFilePermissions(binary, EnumSet.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE));
        return binary;
    }

    public static void setMode(Path tmpFolder, String camName, String mode) throws IOException {
        writeCtl(tmpFolder, camName, "mode", mode);
    }

    public static void writeCtl(Path tmpFolder, String camName, String key, String value) throws IOException {
        String fileName = camName == null
                ? ".fake-ffmpeg-" + key
                : ".fake-ffmpeg-" + camName + "-" + key;
        Files.writeString(tmpFolder.resolve(fileName), value);
    }
}