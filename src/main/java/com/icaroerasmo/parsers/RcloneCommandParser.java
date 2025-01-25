package com.icaroerasmo.parsers;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class RcloneCommandParser {

    private String configLocation;
    private String transferMethod;
    private String sourceFolder;
    private String destinationFolder;
    private List<String> excludePatterns;
    private Boolean ignoreExisting;

    public static RcloneCommandParserBuilder builder() {
        return new RcloneCommandParserBuilder();
    }

    public static class RcloneCommandParserBuilder {

        private RcloneCommandParser rcloneCommandParser;

        public RcloneCommandParserBuilder() {
            this.rcloneCommandParser = new RcloneCommandParser();
        }

        public RcloneCommandParserBuilder configLocation(String configLocation) {
            rcloneCommandParser.setConfigLocation(configLocation);
            return this;
        }

        public RcloneCommandParserBuilder transferMethod(String transferMethod) {
            rcloneCommandParser.setTransferMethod(transferMethod);
            return this;
        }

        public RcloneCommandParserBuilder sourceFolder(String sourceFolder) {
            rcloneCommandParser.setSourceFolder(sourceFolder);
            return this;
        }

        public RcloneCommandParserBuilder destinationFolder(String destinationFolder) {
            rcloneCommandParser.setDestinationFolder(destinationFolder);
            return this;
        }

        public RcloneCommandParserBuilder excludePatterns(List<String> excludePatterns) {
            rcloneCommandParser.setExcludePatterns(excludePatterns);
            return this;
        }

        public RcloneCommandParserBuilder ignoreExisting(Boolean ignoreExisting) {
            rcloneCommandParser.setIgnoreExisting(ignoreExisting);
            return this;
        }

        public List<String> buildAsList() {
            List<String> command = new ArrayList<>();
            command.add("rclone");
            command.add("-v");
            command.add("--config=" + rcloneCommandParser.getConfigLocation());

            // Add transfer method
            command.add(rcloneCommandParser.getTransferMethod());

            // Add source and destination folders
            command.add(rcloneCommandParser.getSourceFolder());
            command.add(rcloneCommandParser.getDestinationFolder());

            // Add exclude patterns
            for (String pattern : rcloneCommandParser.getExcludePatterns()) {
                command.add("--exclude=" + pattern);
            }

            // Add ignore existing flag
            if (rcloneCommandParser.getIgnoreExisting()) {
                command.add("--ignore-existing");
            }

            return command;
        }

        public String build() {
            return String.join(" ", buildAsList());
        }
    }
}