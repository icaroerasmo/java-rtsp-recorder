package com.icaroerasmo.properties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.core.env.MapPropertySource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Binds {@code @ConfigurationProperties} records through Spring Boot's {@link Binder}
 * without starting the application context or touching the network.
 */
class PropertiesBindingTest {

    private static Binder binderFor(Map<String, Object> properties) {
        Iterable<ConfigurationPropertySource> sources =
                ConfigurationPropertySources.from(new MapPropertySource("test", properties));
        return new Binder(sources);
    }

    @Test
    void bindsRtspProperties() {
        RtspProperties props = new RtspProperties();

        Map<String, Object> map = Map.ofEntries(
                Map.entry("rtsp.timeout", "10s"),
                Map.entry("rtsp.video-duration", "10m"),
                Map.entry("rtsp.hardware-acceleration", "NVIDIA"),
                Map.entry("rtsp.vaapi-device", "/dev/dri/renderD129"),
                Map.entry("rtsp.max-retries", "5"),
                Map.entry("rtsp.retry-wait", "10m"),
                Map.entry("rtsp.binary-path", "/usr/local/bin/ffmpeg"),
                Map.entry("rtsp.cameras[0].name", "garage"),
                Map.entry("rtsp.cameras[0].url", "rtsp://user:pass@192.168.0.10:8554/live"),
                Map.entry("rtsp.cameras[0].host", "192.168.0.10"),
                Map.entry("rtsp.cameras[0].port", "8554"),
                Map.entry("rtsp.cameras[0].format", "live"),
                Map.entry("rtsp.cameras[0].protocol", "UDP")
        );

        binderFor(map).bind("rtsp", Bindable.ofInstance(props));

        assertEquals("10s", props.getTimeout());
        assertEquals("10m", props.getVideoDuration());
        assertEquals(RtspProperties.HardwareAcceleration.NVIDIA, props.getHardwareAcceleration());
        assertEquals("/dev/dri/renderD129", props.getVaapiDevice());
        assertEquals(5, props.getMaxRetries());
        assertEquals("10m", props.getRetryWait());
        assertEquals("/usr/local/bin/ffmpeg", props.getBinaryPath());

        List<RtspProperties.Camera> cameras = props.getCameras();
        assertEquals(1, cameras.size());
        RtspProperties.Camera camera = cameras.get(0);
        assertEquals("garage", camera.getName());
        assertEquals("rtsp://user:pass@192.168.0.10:8554/live", camera.getUrl());
        assertEquals(RtspProperties.TransportProtocol.UDP, camera.getProtocol());
    }

    @Test
    void unboundRtspFieldsKeepDefaults() {
        RtspProperties props = new RtspProperties();

        Map<String, Object> map = Map.of("rtsp.timeout", "10s");
        binderFor(map).bind("rtsp", Bindable.ofInstance(props));

        assertEquals("10s", props.getTimeout());
        // Defaults untouched
        assertEquals("5m", props.getVideoDuration());
        assertEquals(RtspProperties.HardwareAcceleration.COPY, props.getHardwareAcceleration());
        assertNull(props.getCameras());
    }

    @Test
    void bindsRcloneProperties() {
        RcloneProperties props = new RcloneProperties();

        Map<String, Object> map = Map.of(
                "rclone.config-location", "/tmp/rclone.conf",
                "rclone.transfer-method", "sync",
                "rclone.destination-folder", "remote:records",
                "rclone.exclude-patterns[0]", "*.tmp",
                "rclone.exclude-patterns[1]", "*.part",
                "rclone.ignore-existing", "true"
        );

        binderFor(map).bind("rclone", Bindable.ofInstance(props));

        assertEquals("/tmp/rclone.conf", props.getConfigLocation());
        assertEquals("sync", props.getTransferMethod());
        assertEquals("remote:records", props.getDestinationFolder());
        assertEquals(List.of("*.tmp", "*.part"), props.getExcludePatterns());
        assertTrue(props.isIgnoreExisting());
    }

    @Test
    void unboundRcloneFieldsKeepDefaults() {
        RcloneProperties props = new RcloneProperties();

        Map<String, Object> map = Map.of("rclone.transfer-method", "move");
        binderFor(map).bind("rclone", Bindable.ofInstance(props));

        assertEquals("move", props.getTransferMethod());
        // Unbound fields keep their defaults
        assertEquals("/app/config/rclone.conf", props.getConfigLocation());
        assertEquals("0 */10 * * * *", props.getSyncCron());
        assertFalse(props.isIgnoreExisting());
        assertTrue(props.getExcludePatterns().isEmpty());
        assertNull(props.getDestinationFolder());
    }
}
