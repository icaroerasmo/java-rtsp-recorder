package com.icaroerasmo.parsers;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RcloneDeleteCommandParser implements CommandParser {

    private String folder;

    public static RcloneDeleteCommandParser.RcloneDeleteCommandParserBuilder builder() {
        return new RcloneDeleteCommandParser.RcloneDeleteCommandParserBuilder();
    }

    public static class RcloneDeleteCommandParserBuilder implements CommandParserBuilder {

        private RcloneDeleteCommandParser rcloneDeleteCommandParser;

        public RcloneDeleteCommandParserBuilder folder(String folder) {
            rcloneDeleteCommandParser.setFolder(folder);
            return this;
        }

        @Override
        public List<String> buildAsList() {

            List<String> command = new ArrayList<>();
            command.add("rclone");
            command.add("delete");
            command.add(rcloneDeleteCommandParser.getFolder());
            command.add("--min-age");
            command.add("20d");
            command.add("&&");
            command.add("rclone");
            command.add("rmdirs");
            command.add(rcloneDeleteCommandParser.getFolder());
            command.add("&&");
            command.add("rclone");
            command.add("dedupe");
            command.add(rcloneDeleteCommandParser.getFolder());

            return command;
        }
    }
}
