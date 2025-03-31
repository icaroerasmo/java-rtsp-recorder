package com.icaroerasmo.parsers;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RcloneDeleteCommandParser implements CommandParser {

    private String configLocation;
    private String folder;
    private String maxAgeVideoFiles;

    public static RcloneDeleteCommandParser.RcloneDeleteCommandParserBuilder builder() {
        return new RcloneDeleteCommandParser.RcloneDeleteCommandParserBuilder();
    }

    public static class RcloneDeleteCommandParserBuilder implements CommandParserBuilder {

        private final RcloneDeleteCommandParser rcloneDeleteCommandParser;

        public RcloneDeleteCommandParserBuilder() {
            this.rcloneDeleteCommandParser = new RcloneDeleteCommandParser();
        }

        public RcloneDeleteCommandParserBuilder configLocation(String configLocation) {
            rcloneDeleteCommandParser.setConfigLocation(configLocation);
            return this;
        }

        public RcloneDeleteCommandParserBuilder folder(String folder) {
            rcloneDeleteCommandParser.setFolder(folder);
            return this;
        }

        public RcloneDeleteCommandParserBuilder maxAgeVideoFiles(String maxAgeVideoFiles) {
            rcloneDeleteCommandParser.setMaxAgeVideoFiles(maxAgeVideoFiles);
            return this;
        }

        @Override
        public List<String> buildAsList() {

            List<String> command = new ArrayList<>();
            command.add("rclone");
            command.add("-v");
            command.add("--config=" + rcloneDeleteCommandParser.getConfigLocation());
            command.add("delete");
            command.add(rcloneDeleteCommandParser.getFolder());
            command.add("--min-age");
            command.add(rcloneDeleteCommandParser.getMaxAgeVideoFiles());

            return command;
        }
    }
}
