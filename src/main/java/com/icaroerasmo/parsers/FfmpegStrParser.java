package com.icaroerasmo.parsers;

import com.icaroerasmo.properties.RtspProperties;
import com.icaroerasmo.util.PropertiesUtil;
import lombok.Data;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
public class FfmpegStrParser {

    private String timeout;
    private String url;
    private Integer doneSegmentsListSize;
    private String videoDuration;
    private String transportProtocol;
    private String tmpPath;
    private String cameraName;

    public static FfmpegStrParserBuilder builder() {
        return new FfmpegStrParserBuilder();
    }

    public static class FfmpegStrParserBuilder {

        private final PropertiesUtil propertiesUtil = new PropertiesUtil();

        private final FfmpegStrParser ffmpegStrParser;

        public FfmpegStrParserBuilder() {
            this.ffmpegStrParser = new FfmpegStrParser();
        }

        public FfmpegStrParserBuilder timeout(String timeout) {
            ffmpegStrParser.setTimeout(timeout);
            return this;
        }

        public FfmpegStrParserBuilder url(String url) {
            ffmpegStrParser.setUrl(url);
            return this;
        }

        public FfmpegStrParserBuilder doneSegmentsListSize(Integer doneSegmentsListSize) {
            ffmpegStrParser.setDoneSegmentsListSize(doneSegmentsListSize);
            return this;
        }

        public FfmpegStrParserBuilder videoDuration(String videoDuration) {
            ffmpegStrParser.setVideoDuration(videoDuration);
            return this;
        }

        public FfmpegStrParserBuilder transportProtocol(RtspProperties.TransportProtocol transportProtocol) {
            return transportProtocol(transportProtocol.name().toLowerCase());
        }

        public FfmpegStrParserBuilder transportProtocol(String transportProtocol) {
            ffmpegStrParser.setTransportProtocol(transportProtocol);
            return this;
        }

        public FfmpegStrParserBuilder tmpPath(String tmpPath) {
            ffmpegStrParser.setTmpPath(tmpPath);
            return this;
        }

        public FfmpegStrParserBuilder cameraName(String cameraName) {
            ffmpegStrParser.setCameraName(cameraName);
            return this;
        }

        public String build() {

            if(ffmpegStrParser.getUrl() == null || ffmpegStrParser.getUrl().isBlank()) {
                throw new IllegalArgumentException("Url is required");
            }

            if(ffmpegStrParser.getTmpPath() == null || ffmpegStrParser.getTmpPath().isBlank()) {
                throw new IllegalArgumentException("TmpPath is required");
            }

            if (ffmpegStrParser.getCameraName() == null || ffmpegStrParser.getCameraName().isBlank()) {
                throw new IllegalArgumentException("CameraName is required");
            }

            StringBuilder strBuilder = new StringBuilder();

            strBuilder.append("ffmpeg ");

            strBuilder.append("-rtsp_transport %s ".formatted(ffmpegStrParser.getTransportProtocol()));

            strBuilder.append("-i %s -c copy ".formatted(ffmpegStrParser.getUrl()));

            if(ffmpegStrParser.getTimeout() != null) {
                strBuilder.append("-rw_timeout %s ".formatted(propertiesUtil.durationParser(ffmpegStrParser.getTimeout(), TimeUnit.MILLISECONDS)));
            }

            if(ffmpegStrParser.getDoneSegmentsListSize() != null &&
                    ffmpegStrParser.getVideoDuration() != null) {

                strBuilder.append("-f segment ");

                final String doneSegmentsList = ffmpegStrParser.getTmpPath() +
                        "/.%s_done_segments".formatted(ffmpegStrParser.getCameraName());

                strBuilder.append("-segment_list %s ".formatted(doneSegmentsList));
                strBuilder.append("-segment_list_size %s ".formatted(ffmpegStrParser.getDoneSegmentsListSize()));
                strBuilder.append("-strftime 1 ");
                strBuilder.append("-segment_time %s ".formatted(
                        propertiesUtil.durationParser(ffmpegStrParser.getVideoDuration(), TimeUnit.SECONDS)));
                strBuilder.append("-reset_timestamps 1 ");

            }

            strBuilder.append("%s/%s%%Y-%%m-%%d_%%H-%%M-%%S.mkv".
                    formatted(ffmpegStrParser.getTmpPath(), ffmpegStrParser.getCameraName()));

            return strBuilder.toString();
        }
    }
}
