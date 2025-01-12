package com.icaroerasmo.parsers;

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
    private String tmpPath;
    private String cameraName;

    public static FfmpegStrParserBuilder builder() {
        return new FfmpegStrParserBuilder();
    }

    public static class FfmpegStrParserBuilder {

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

            strBuilder.append("-rtsp_transport udp ");

            if(ffmpegStrParser.getTimeout() != null) {
                strBuilder.append("-timeout %s ".formatted(durationParser(ffmpegStrParser.getTimeout(), TimeUnit.MILLISECONDS)));
            }

            strBuilder.append("-i %s -c copy -f segment ".formatted(ffmpegStrParser.getUrl()));

            if(ffmpegStrParser.getCameraName() != null &&
                    ffmpegStrParser.getDoneSegmentsListSize() != null &&
                    ffmpegStrParser.getVideoDuration() != null) {

                final String doneSegmentsList = ffmpegStrParser.getTmpPath() +
                        "/.%s_done_segments".formatted(ffmpegStrParser.getCameraName());

                strBuilder.append("-segment_list %s ".formatted(doneSegmentsList));
                strBuilder.append("-segment_list_size %s ".formatted(ffmpegStrParser.getDoneSegmentsListSize()));
                strBuilder.append("-strftime 1 ");
                strBuilder.append("-segment_time %s ".formatted(durationParser(ffmpegStrParser.getVideoDuration(), TimeUnit.SECONDS)));
                strBuilder.append("-reset_timestamps 1 ");

            }

            strBuilder.append("%s/%s%%Y-%%m-%%d_%%H-%%M-%%S.mkv ".formatted(ffmpegStrParser.getTmpPath(), ffmpegStrParser.getCameraName()));

            return strBuilder.toString();
        }

        private String durationParser(String duration, TimeUnit timeUnit) {
            Pattern pattern = Pattern.compile("(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?");
            Matcher matcher = pattern.matcher(duration);
            long milliseconds = 0;

            if (matcher.matches()) {
                String hours = matcher.group(1);
                String minutes = matcher.group(2);
                String seconds = matcher.group(3);

                if (hours != null) {
                    milliseconds += Long.parseLong(hours) * 3600000;
                }
                if (minutes != null) {
                    milliseconds += Long.parseLong(minutes) * 60000;
                }
                if (seconds != null) {
                    milliseconds += Long.parseLong(seconds) * 1000;
                }
            }

            return String.valueOf(timeUnit.convert(milliseconds, TimeUnit.MILLISECONDS));
        }
    }
}
