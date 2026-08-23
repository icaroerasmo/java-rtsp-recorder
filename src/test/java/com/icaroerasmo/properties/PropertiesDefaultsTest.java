package com.icaroerasmo.properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropertiesDefaultsTest {

    @Test
    void rtspPropertiesDefaults() {
        RtspProperties props = new RtspProperties();

        assertEquals("5s", props.getTimeout());
        assertEquals("5m", props.getVideoDuration());
        assertEquals(RtspProperties.HardwareAcceleration.COPY, props.getHardwareAcceleration());
        assertEquals("/dev/dri/renderD128", props.getVaapiDevice());
        assertNull(props.getCameras());
    }

    @Test
    void cameraDefaults() {
        RtspProperties.Camera camera = new RtspProperties.Camera();

        assertEquals(RtspProperties.TransportProtocol.TCP, camera.getProtocol());
        assertNull(camera.getUrl());
        assertNull(camera.getName());
        assertNull(camera.getHost());
        assertNull(camera.getPort());
        assertNull(camera.getFormat());
        assertNull(camera.getUsername());
        assertNull(camera.getPassword());
    }

    @Test
    void storagePropertiesDefaults() {
        StorageProperties props = new StorageProperties();

        assertEquals("5m", props.getFileMoverInterval());
        assertEquals("0 30 0 * * *", props.getDeleteOldFilesCron());
        assertEquals("/app/data/tmp", props.getTmpFolder());
        assertEquals("/app/data/records", props.getRecordsFolder());
        assertEquals("10G", props.getMaxRecordsFolderSize());
        assertEquals("20d", props.getMaxAgeRemoteVideoFiles());
    }

    @Test
    void rclonePropertiesDefaults() {
        RcloneProperties props = new RcloneProperties();

        assertEquals("/app/config/rclone.conf", props.getConfigLocation());
        assertEquals("0 0 0 * * *", props.getDeleteCron());
        assertEquals("0 10 0 * * *", props.getRmdirsCron());
        assertEquals("0 20 0 * * *", props.getDedupeCron());
        assertEquals("0 */10 * * * *", props.getSyncCron());
        assertEquals("copy", props.getTransferMethod());
        assertNull(props.getDestinationFolder());
        assertTrue(props.getExcludePatterns().isEmpty());
        assertFalse(props.isIgnoreExisting());
    }

    @Test
    void generalPropertiesDefaults() {
        GeneralProperties props = new GeneralProperties();

        assertEquals("en_GB", props.getLocale());
        assertNull(props.getTimezone());
    }
}
