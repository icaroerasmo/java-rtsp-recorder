package com.icaroerasmo.parsers;

import com.icaroerasmo.properties.RtspProperties;
import com.icaroerasmo.util.PropertiesUtil;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Data
public class FfmpegCommandParser implements CommandParser {

    private String timeout;
    private String url;
    private Integer doneSegmentsListSize;
    private String videoDuration;
    private String transportProtocol;
    private String tmpPath;
    private String cameraName;
    private RtspProperties.HardwareAcceleration hardwareAcceleration;
    private String vaapiDevice;

    public static FfmpegCommandParserBuilder builder() {
        return new FfmpegCommandParserBuilder();
    }

    public static class FfmpegCommandParserBuilder implements CommandParserBuilder {

        private final PropertiesUtil propertiesUtil = new PropertiesUtil();
        private final FfmpegCommandParser ffmpegCommandParser;

        public FfmpegCommandParserBuilder() {
            this.ffmpegCommandParser = new FfmpegCommandParser();
        }

        public FfmpegCommandParserBuilder timeout(String timeout) {
            ffmpegCommandParser.setTimeout(timeout);
            return this;
        }

        public FfmpegCommandParserBuilder url(String url) {
            ffmpegCommandParser.setUrl(url);
            return this;
        }

        public FfmpegCommandParserBuilder doneSegmentsListSize(Integer doneSegmentsListSize) {
            ffmpegCommandParser.setDoneSegmentsListSize(doneSegmentsListSize);
            return this;
        }

        public FfmpegCommandParserBuilder videoDuration(String videoDuration) {
            ffmpegCommandParser.setVideoDuration(videoDuration);
            return this;
        }

        public FfmpegCommandParserBuilder transportProtocol(RtspProperties.TransportProtocol transportProtocol) {
            return transportProtocol(transportProtocol.name().toLowerCase());
        }

        public FfmpegCommandParserBuilder transportProtocol(String transportProtocol) {
            ffmpegCommandParser.setTransportProtocol(transportProtocol);
            return this;
        }

        public FfmpegCommandParserBuilder tmpPath(String tmpPath) {
            ffmpegCommandParser.setTmpPath(tmpPath);
            return this;
        }

        public FfmpegCommandParserBuilder cameraName(String cameraName) {
            ffmpegCommandParser.setCameraName(cameraName);
            return this;
        }

        public FfmpegCommandParserBuilder hardwareAcceleration(RtspProperties.HardwareAcceleration hardwareAcceleration) {
            ffmpegCommandParser.setHardwareAcceleration(hardwareAcceleration);
            return this;
        }

        public FfmpegCommandParserBuilder vaapiDevice(String vaapiDevice) {
            ffmpegCommandParser.setVaapiDevice(vaapiDevice);
            return this;
        }

        @Override
        public List<String> buildAsList() {
            if (ffmpegCommandParser.getUrl() == null || ffmpegCommandParser.getUrl().isBlank()) {
                throw new IllegalArgumentException("Url is required");
            }

            if (ffmpegCommandParser.getTmpPath() == null || ffmpegCommandParser.getTmpPath().isBlank()) {
                throw new IllegalArgumentException("TmpPath is required");
            }

            if (ffmpegCommandParser.getCameraName() == null || ffmpegCommandParser.getCameraName().isBlank()) {
                throw new IllegalArgumentException("CameraName is required");
            }

            List<String> command = new ArrayList<>();
            command.add("ffmpeg");
            command.add("-nostdin");
            command.add("-rtsp_transport");
            command.add(ffmpegCommandParser.getTransportProtocol());

            if (ffmpegCommandParser.getTimeout() != null) {
                command.add("-timeout");
                command.add(String.valueOf(propertiesUtil.durationParser(ffmpegCommandParser.getTimeout(), TimeUnit.MICROSECONDS)));
            }

            command.add("-i");
            command.add(ffmpegCommandParser.getUrl());
            command.add("-map");
            command.add("0:v:0");
            command.add("-map");
            command.add("0:a?");
            command.add("-dn");
            command.add("-sn");
            command.add("-c:a");
            command.add("copy");
            appendVideoEncoding(command);

            if (ffmpegCommandParser.getDoneSegmentsListSize() != null && ffmpegCommandParser.getVideoDuration() != null) {
                final String segmentDuration = String.valueOf(
                        propertiesUtil.durationParser(ffmpegCommandParser.getVideoDuration(), TimeUnit.SECONDS)
                );

                command.add("-force_key_frames");
                command.add("expr:gte(t,n_forced*" + segmentDuration + ")");
                command.add("-f");
                command.add("segment");

                final String doneSegmentsList = ffmpegCommandParser.getTmpPath() +
                        "/." + ffmpegCommandParser.getCameraName() + "_done_segments";

                command.add("-segment_list");
                command.add(doneSegmentsList);
                command.add("-segment_list_size");
                command.add(String.valueOf(ffmpegCommandParser.getDoneSegmentsListSize()));
                command.add("-strftime");
                command.add("1");
                command.add("-segment_time");
                command.add(segmentDuration);
                command.add("-reset_timestamps");
                command.add("1");
            }

            command.add(ffmpegCommandParser.getTmpPath() + "/" + ffmpegCommandParser.getCameraName() + "%Y-%m-%d_%H-%M-%S.mkv");

            return command;
        }

        private void appendVideoEncoding(List<String> command) {
            RtspProperties.HardwareAcceleration acceleration = ffmpegCommandParser.getHardwareAcceleration();

            if (acceleration == null) {
                acceleration = RtspProperties.HardwareAcceleration.NONE;
            }

            switch (acceleration) {
                case NVIDIA -> appendNvidiaEncoding(command);
                case RADEON -> appendVaapiEncoding(command);
                case NONE -> appendCpuEncoding(command);
            }
        }

        private void appendNvidiaEncoding(List<String> command) {
            command.add("-c:v");
            command.add("h264_nvenc");
            command.add("-preset");
            command.add("p4");
            command.add("-tune");
            command.add("ll");
            command.add("-rc");
            command.add("vbr");
            command.add("-cq");
            command.add("28");
            command.add("-b:v");
            command.add("0");
        }

        private void appendVaapiEncoding(List<String> command) {
            command.add("-vaapi_device");
            command.add(ffmpegCommandParser.getVaapiDevice());
            command.add("-vf");
            command.add("format=nv12,hwupload");
            command.add("-c:v");
            command.add("h264_vaapi");
            command.add("-qp");
            command.add("23");
        }

        private void appendCpuEncoding(List<String> command) {
            command.add("-c:v");
            command.add("libx264");
            command.add("-preset");
            command.add("veryfast");
            command.add("-crf");
            command.add("23");
        }
    }
}
