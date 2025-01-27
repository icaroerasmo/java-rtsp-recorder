package com.icaroerasmo.parsers;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RcloneDedupeCommandParser implements CommandParser {

    private String folder;

    public static RcloneDedupeCommandParser.RcloneDedupCommandParserBuilder builder() {
        return new RcloneDedupeCommandParser.RcloneDedupCommandParserBuilder();
    }

    public static class RcloneDedupCommandParserBuilder implements CommandParserBuilder {

        private final RcloneDedupeCommandParser rcloneDeleteCommandParser;

        public RcloneDedupCommandParserBuilder() {
            this.rcloneDeleteCommandParser = new RcloneDedupeCommandParser();
        }

        public RcloneDedupCommandParserBuilder folder(String folder) {
            rcloneDeleteCommandParser.setFolder(folder);
            return this;
        }

        @Override
        public List<String> buildAsList() {

            List<String> command = new ArrayList<>();
            command.add("rclone");
            command.add("dedupe");
            command.add(rcloneDeleteCommandParser.getFolder());

            return command;
        }
    }
}
