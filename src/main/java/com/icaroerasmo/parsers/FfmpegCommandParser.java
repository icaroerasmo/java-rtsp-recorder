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
            command.add("-i");
            command.add(ffmpegCommandParser.getUrl());
            command.add("-c");
            command.add("copy");

            if (ffmpegCommandParser.getTimeout() != null) {
                command.add("-rw_timeout");
                command.add(String.valueOf(propertiesUtil.durationParser(ffmpegCommandParser.getTimeout(), TimeUnit.MILLISECONDS)));
            }

            if (ffmpegCommandParser.getDoneSegmentsListSize() != null && ffmpegCommandParser.getVideoDuration() != null) {
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
                command.add(String.valueOf(propertiesUtil.durationParser(ffmpegCommandParser.getVideoDuration(), TimeUnit.SECONDS)));
                command.add("-reset_timestamps");
                command.add("1");
            }

            command.add(ffmpegCommandParser.getTmpPath() + "/" + ffmpegCommandParser.getCameraName() + "%Y-%m-%d_%H-%M-%S.mkv");

            return command;
        }
    }
}