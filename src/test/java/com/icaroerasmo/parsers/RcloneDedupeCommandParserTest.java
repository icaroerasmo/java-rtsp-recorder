package com.icaroerasmo.parsers;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RcloneDedupeCommandParserTest {

    @Test
    void buildsDedupeCommand() {
        List<String> command = RcloneDedupeCommandParser.builder()
                .configLocation("/tmp/rclone.conf")
                .folder("remote:records")
                .buildAsList();

        List<String> expected = List.of(
                "rclone",
                "-v",
                "--config=/tmp/rclone.conf",
                "dedupe",
                "remote:records"
        );

        assertEquals(expected, command);
    }

    @Test
    void buildsCommandStringJoinedBySpaces() {
        String command = RcloneDedupeCommandParser.builder()
                .configLocation("/tmp/rclone.conf")
                .folder("remote:records/2024")
                .build();

        assertEquals("rclone -v --config=/tmp/rclone.conf dedupe remote:records/2024", command);
    }
}
