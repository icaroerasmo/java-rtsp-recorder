package com.icaroerasmo.parsers;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RcloneRmdirsCommandParserTest {

    @Test
    void buildsRmdirsCommand() {
        List<String> command = RcloneRmdirsCommandParser.builder()
                .configLocation("/tmp/rclone.conf")
                .folder("remote:records/2024/May")
                .buildAsList();

        List<String> expected = List.of(
                "rclone",
                "-v",
                "--config=/tmp/rclone.conf",
                "rmdirs",
                "remote:records/2024/May"
        );

        assertEquals(expected, command);
    }

    @Test
    void buildsCommandStringJoinedBySpaces() {
        String command = RcloneRmdirsCommandParser.builder()
                .configLocation("/tmp/rclone.conf")
                .folder("remote:records")
                .build();

        assertEquals("rclone -v --config=/tmp/rclone.conf rmdirs remote:records", command);
    }
}
