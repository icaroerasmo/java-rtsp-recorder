package com.icaroerasmo.parsers;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RcloneSyncCommandParser implements CommandParser {

    private String configLocation;
    private String transferMethod;
    private String sourceFolder;
    private String destinationFolder;
    private List<String> excludePatterns;
    private Boolean ignoreExisting;

    public static RcloneSyncCommandParserBuilder builder() {
        return new RcloneSyncCommandParserBuilder();
    }

    public static class RcloneSyncCommandParserBuilder implements CommandParserBuilder {

        private final RcloneSyncCommandParser rcloneSyncCommandParser;

        public RcloneSyncCommandParserBuilder() {
            this.rcloneSyncCommandParser = new RcloneSyncCommandParser();
        }

        public RcloneSyncCommandParserBuilder configLocation(String configLocation) {
            rcloneSyncCommandParser.setConfigLocation(configLocation);
            return this;
        }

        public RcloneSyncCommandParserBuilder transferMethod(String transferMethod) {
            rcloneSyncCommandParser.setTransferMethod(transferMethod);
            return this;
        }

        public RcloneSyncCommandParserBuilder sourceFolder(String sourceFolder) {
            rcloneSyncCommandParser.setSourceFolder(sourceFolder);
            return this;
        }

        public RcloneSyncCommandParserBuilder destinationFolder(String destinationFolder) {
            rcloneSyncCommandParser.setDestinationFolder(destinationFolder);
            return this;
        }

        public RcloneSyncCommandParserBuilder excludePatterns(List<String> excludePatterns) {
            rcloneSyncCommandParser.setExcludePatterns(excludePatterns);
            return this;
        }

        public RcloneSyncCommandParserBuilder ignoreExisting(Boolean ignoreExisting) {
            rcloneSyncCommandParser.setIgnoreExisting(ignoreExisting);
            return this;
        }

        @Override
        public List<String> buildAsList() {
            List<String> command = new ArrayList<>();
            command.add("rclone");
            command.add("-v");
            command.add("--config=" + rcloneSyncCommandParser.getConfigLocation());

            // Add transfer method
            command.add(rcloneSyncCommandParser.getTransferMethod());

            // Add source and destination folders
            command.add(rcloneSyncCommandParser.getSourceFolder());
            command.add(rcloneSyncCommandParser.getDestinationFolder());

            // Add exclude patterns
            for (String pattern : rcloneSyncCommandParser.getExcludePatterns()) {
                command.add("--exclude=" + pattern);
            }

            // Add ignore existing flag
            if (rcloneSyncCommandParser.getIgnoreExisting()) {
                command.add("--ignore-existing");
            }

            return command;
        }
    }
}