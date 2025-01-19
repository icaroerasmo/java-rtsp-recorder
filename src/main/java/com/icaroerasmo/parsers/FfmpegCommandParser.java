package com.icaroerasmo.parsers;

import com.icaroerasmo.properties.RtspProperties;
import com.icaroerasmo.util.PropertiesUtil;
import lombok.Data;

import java.util.concurrent.TimeUnit;

@Data
public class FfmpegCommandParser {

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

    public static class FfmpegCommandParserBuilder {

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

        public String build() {

            if(ffmpegCommandParser.getUrl() == null || ffmpegCommandParser.getUrl().isBlank()) {
                throw new IllegalArgumentException("Url is required");
            }

            if(ffmpegCommandParser.getTmpPath() == null || ffmpegCommandParser.getTmpPath().isBlank()) {
                throw new IllegalArgumentException("TmpPath is required");
            }

            if (ffmpegCommandParser.getCameraName() == null || ffmpegCommandParser.getCameraName().isBlank()) {
                throw new IllegalArgumentException("CameraName is required");
            }

            StringBuilder strBuilder = new StringBuilder();

            strBuilder.append("ffmpeg ");

            strBuilder.append("-rtsp_transport %s ".formatted(ffmpegCommandParser.getTransportProtocol()));

            strBuilder.append("-i %s -c copy ".formatted(ffmpegCommandParser.getUrl()));

            if(ffmpegCommandParser.getTimeout() != null) {
                strBuilder.append("-rw_timeout %s ".formatted(propertiesUtil.durationParser(ffmpegCommandParser.getTimeout(), TimeUnit.MILLISECONDS)));
            }

            if(ffmpegCommandParser.getDoneSegmentsListSize() != null &&
                    ffmpegCommandParser.getVideoDuration() != null) {

                strBuilder.append("-f segment ");

                final String doneSegmentsList = ffmpegCommandParser.getTmpPath() +
                        "/.%s_done_segments".formatted(ffmpegCommandParser.getCameraName());

                strBuilder.append("-segment_list %s ".formatted(doneSegmentsList));
                strBuilder.append("-segment_list_size %s ".formatted(ffmpegCommandParser.getDoneSegmentsListSize()));
                strBuilder.append("-strftime 1 ");
                strBuilder.append("-segment_time %s ".formatted(
                        propertiesUtil.durationParser(ffmpegCommandParser.getVideoDuration(), TimeUnit.SECONDS)));
                strBuilder.append("-reset_timestamps 1 ");

            }

            strBuilder.append("%s/%s%%Y-%%m-%%d_%%H-%%M-%%S.mkv".
                    formatted(ffmpegCommandParser.getTmpPath(), ffmpegCommandParser.getCameraName()));

            return strBuilder.toString();
        }
    }
}
