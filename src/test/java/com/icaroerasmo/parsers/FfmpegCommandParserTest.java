package com.icaroerasmo.parsers;

import com.icaroerasmo.properties.RtspProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FfmpegCommandParserTest {

    private static final String URL = "rtsp://user:pass@192.168.0.10:8554/live";
    private static final String TMP_PATH = "/tmp/records";
    private static final String CAMERA_NAME = "cam1";

    private FfmpegCommandParser.FfmpegCommandParserBuilder fullCopyBuilder() {
        return FfmpegCommandParser.builder()
                .url(URL)
                .tmpPath(TMP_PATH)
                .cameraName(CAMERA_NAME)
                .transportProtocol(RtspProperties.TransportProtocol.TCP)
                .doneSegmentsListSize(20)
                .videoDuration("5m")
                .timeout("5s")
                .hardwareAcceleration(RtspProperties.HardwareAcceleration.COPY);
    }

    @Test
    void buildsFullSegmentCommandWithCopyEncoding() {
        List<String> command = fullCopyBuilder().buildAsList();

        List<String> expected = List.of(
                "ffmpeg",
                "-nostdin",
                "-fflags",
                "+genpts+discardcorrupt",
                "-rtsp_transport",
                "tcp",
                "-timeout",
                "5000000",
                "-i",
                URL,
                "-map",
                "0:v:0",
                "-map",
                "0:a?",
                "-dn",
                "-sn",
                "-c",
                "copy",
                "-force_key_frames",
                "expr:gte(t,n_forced*300)",
                "-f",
                "segment",
                "-segment_list",
                TMP_PATH + "/." + CAMERA_NAME + "_done_segments",
                "-segment_list_size",
                "20",
                "-break_non_keyframes",
                "1",
                "-strftime",
                "1",
                "-segment_time",
                "300",
                "-reset_timestamps",
                "1",
                TMP_PATH + "/" + CAMERA_NAME + "%Y-%m-%d_%H-%M-%S.mkv"
        );

        assertEquals(expected, command);
    }

    @Test
    void buildsCommandStringJoinedBySpaces() {
        String command = fullCopyBuilder().build();

        assertTrue(command.startsWith("ffmpeg -nostdin -fflags +genpts+discardcorrupt -rtsp_transport tcp"));
        assertTrue(command.contains(" -c copy "));
        assertTrue(command.endsWith(TMP_PATH + "/" + CAMERA_NAME + "%Y-%m-%d_%H-%M-%S.mkv"));
    }

    @Test
    void usesLowercaseTransportProtocolFromEnumOverload() {
        List<String> command = FfmpegCommandParser.builder()
                .url(URL)
                .tmpPath(TMP_PATH)
                .cameraName(CAMERA_NAME)
                .transportProtocol(RtspProperties.TransportProtocol.UDP)
                .buildAsList();

        assertEquals("udp", command.get(command.indexOf("-rtsp_transport") + 1));
    }

    @Test
    void omitsSegmentArgsWhenDoneSegmentsListSizeMissing() {
        List<String> command = FfmpegCommandParser.builder()
                .url(URL)
                .tmpPath(TMP_PATH)
                .cameraName(CAMERA_NAME)
                .transportProtocol(RtspProperties.TransportProtocol.TCP)
                .videoDuration("5m")
                .hardwareAcceleration(RtspProperties.HardwareAcceleration.NONE)
                .buildAsList();

        assertTrue(command.contains("-c"));
        assertTrue(command.contains("copy"));
        assertTrue(command.contains("-i"));
        // No segment-related flags at all
        assertTrue(command.stream().noneMatch("-segment_list"::equals));
        assertTrue(command.stream().noneMatch("-segment_time"::equals));
        assertTrue(command.stream().noneMatch("-force_key_frames"::equals));
        // Output file is still appended
        assertEquals(TMP_PATH + "/" + CAMERA_NAME + "%Y-%m-%d_%H-%M-%S.mkv",
                command.get(command.size() - 1));
    }

    @Test
    void omitsSegmentArgsWhenVideoDurationMissing() {
        List<String> command = FfmpegCommandParser.builder()
                .url(URL)
                .tmpPath(TMP_PATH)
                .cameraName(CAMERA_NAME)
                .transportProtocol(RtspProperties.TransportProtocol.TCP)
                .doneSegmentsListSize(20)
                .hardwareAcceleration(RtspProperties.HardwareAcceleration.NONE)
                .buildAsList();

        assertTrue(command.stream().noneMatch("-segment_list_size"::equals));
        assertTrue(command.stream().noneMatch("-segment_time"::equals));
    }

    @Test
    void omitsTimeoutArgWhenTimeoutNull() {
        List<String> command = FfmpegCommandParser.builder()
                .url(URL)
                .tmpPath(TMP_PATH)
                .cameraName(CAMERA_NAME)
                .transportProtocol(RtspProperties.TransportProtocol.TCP)
                .hardwareAcceleration(RtspProperties.HardwareAcceleration.NONE)
                .buildAsList();

        assertTrue(command.stream().noneMatch("-timeout"::equals));
    }

    @Test
    void buildsNvidiaEncodingFlags() {
        List<String> command = FfmpegCommandParser.builder()
                .url(URL)
                .tmpPath(TMP_PATH)
                .cameraName(CAMERA_NAME)
                .transportProtocol(RtspProperties.TransportProtocol.TCP)
                .hardwareAcceleration(RtspProperties.HardwareAcceleration.NVIDIA)
                .buildAsList();

        assertTrue(command.containsAll(List.of(
                "-c:v", "h264_nvenc", "-preset", "p4", "-tune", "ll",
                "-rc", "vbr", "-cq", "28", "-b:v", "0", "-forced-idr", "1",
                "-c:a", "copy")));
    }

    @Test
    void buildsCpuEncodingFlags() {
        List<String> command = FfmpegCommandParser.builder()
                .url(URL)
                .tmpPath(TMP_PATH)
                .cameraName(CAMERA_NAME)
                .transportProtocol(RtspProperties.TransportProtocol.TCP)
                .hardwareAcceleration(RtspProperties.HardwareAcceleration.CPU)
                .buildAsList();

        assertTrue(command.containsAll(List.of(
                "-c:v", "libx264", "-preset", "veryfast", "-crf", "23",
                "-c:a", "copy")));
    }

    @Test
    void buildsVaapiEncodingFlagsWithDevice() {
        List<String> command = FfmpegCommandParser.builder()
                .url(URL)
                .tmpPath(TMP_PATH)
                .cameraName(CAMERA_NAME)
                .transportProtocol(RtspProperties.TransportProtocol.TCP)
                .hardwareAcceleration(RtspProperties.HardwareAcceleration.RADEON)
                .vaapiDevice("/dev/dri/renderD128")
                .buildAsList();

        assertTrue(command.containsAll(List.of(
                "-vaapi_device", "/dev/dri/renderD128",
                "-vf", "format=nv12,hwupload",
                "-c:v", "h264_vaapi", "-qp", "23", "-c:a", "copy")));
    }

    @Test
    void defaultsToCopyEncodingWhenAccelerationNull() {
        List<String> command = FfmpegCommandParser.builder()
                .url(URL)
                .tmpPath(TMP_PATH)
                .cameraName(CAMERA_NAME)
                .transportProtocol(RtspProperties.TransportProtocol.TCP)
                .buildAsList();

        assertTrue(command.containsAll(List.of("-c", "copy")));
    }

    @Test
    void convertsVideoDurationToSecondsForSegmentTime() {
        List<String> command = FfmpegCommandParser.builder()
                .url(URL)
                .tmpPath(TMP_PATH)
                .cameraName(CAMERA_NAME)
                .transportProtocol(RtspProperties.TransportProtocol.TCP)
                .doneSegmentsListSize(5)
                .videoDuration("1m30s")
                .hardwareAcceleration(RtspProperties.HardwareAcceleration.NONE)
                .buildAsList();

        int segmentTimeIdx = command.indexOf("-segment_time");
        assertEquals("90", command.get(segmentTimeIdx + 1));
        assertTrue(command.contains("expr:gte(t,n_forced*90)"));
    }

    @Test
    void throwsWhenUrlMissing() {
        FfmpegCommandParser.FfmpegCommandParserBuilder builder = FfmpegCommandParser.builder()
                .tmpPath(TMP_PATH)
                .cameraName(CAMERA_NAME)
                .transportProtocol(RtspProperties.TransportProtocol.TCP);

        assertThrows(IllegalArgumentException.class, builder::buildAsList);
    }

    @Test
    void throwsWhenUrlBlank() {
        FfmpegCommandParser.FfmpegCommandParserBuilder builder = FfmpegCommandParser.builder()
                .url("   ")
                .tmpPath(TMP_PATH)
                .cameraName(CAMERA_NAME)
                .transportProtocol(RtspProperties.TransportProtocol.TCP);

        assertThrows(IllegalArgumentException.class, builder::buildAsList);
    }

    @Test
    void throwsWhenTmpPathMissing() {
        FfmpegCommandParser.FfmpegCommandParserBuilder builder = FfmpegCommandParser.builder()
                .url(URL)
                .cameraName(CAMERA_NAME)
                .transportProtocol(RtspProperties.TransportProtocol.TCP);

        assertThrows(IllegalArgumentException.class, builder::buildAsList);
    }

    @Test
    void throwsWhenCameraNameMissing() {
        FfmpegCommandParser.FfmpegCommandParserBuilder builder = FfmpegCommandParser.builder()
                .url(URL)
                .tmpPath(TMP_PATH)
                .transportProtocol(RtspProperties.TransportProtocol.TCP);

        assertThrows(IllegalArgumentException.class, builder::buildAsList);
    }
}
