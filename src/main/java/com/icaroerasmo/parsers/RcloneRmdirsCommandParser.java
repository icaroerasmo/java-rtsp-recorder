package com.icaroerasmo.parsers;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RcloneRmdirsCommandParser implements CommandParser {

    private String folder;

    public static RcloneRmdirsCommandParser.RcloneRmdirsCommandParserBuilder builder() {
        return new RcloneRmdirsCommandParser.RcloneRmdirsCommandParserBuilder();
    }

    public static class RcloneRmdirsCommandParserBuilder implements CommandParserBuilder {

        private final RcloneRmdirsCommandParser rcloneDeleteCommandParser;

        public RcloneRmdirsCommandParserBuilder() {
            this.rcloneDeleteCommandParser = new RcloneRmdirsCommandParser();
        }

        public RcloneRmdirsCommandParserBuilder folder(String folder) {
            rcloneDeleteCommandParser.setFolder(folder);
            return this;
        }

        @Override
        public List<String> buildAsList() {

            List<String> command = new ArrayList<>();
            command.add("rclone");
            command.add("rmdirs");
            command.add(rcloneDeleteCommandParser.getFolder());

            return command;
        }
    }
}
