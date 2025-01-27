package com.icaroerasmo.parsers;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RcloneDedupeCommandParser implements CommandParser {

    private String configLocation;
    private String folder;

    public static RcloneDedupeCommandParserBuilder builder() {
        return new RcloneDedupeCommandParserBuilder();
    }

    public static class RcloneDedupeCommandParserBuilder implements CommandParserBuilder {

        private final RcloneDedupeCommandParser rcloneDeleteCommandParser;

        public RcloneDedupeCommandParserBuilder() {
            this.rcloneDeleteCommandParser = new RcloneDedupeCommandParser();
        }

        public RcloneDedupeCommandParserBuilder configLocation(String configLocation) {
            rcloneDeleteCommandParser.setConfigLocation(configLocation);
            return this;
        }

        public RcloneDedupeCommandParserBuilder folder(String folder) {
            rcloneDeleteCommandParser.setFolder(folder);
            return this;
        }

        @Override
        public List<String> buildAsList() {

            List<String> command = new ArrayList<>();
            command.add("rclone");
            command.add("-v");
            command.add("--config=" + rcloneDeleteCommandParser.getConfigLocation());
            command.add("dedupe");
            command.add(rcloneDeleteCommandParser.getFolder());

            return command;
        }
    }
}
