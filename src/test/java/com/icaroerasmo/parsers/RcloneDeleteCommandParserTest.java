package com.icaroerasmo.parsers;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RcloneDeleteCommandParserTest {

    @Test
    void buildsDeleteCommandWithMinAge() {
        List<String> command = RcloneDeleteCommandParser.builder()
                .configLocation("/tmp/rclone.conf")
                .folder("remote:records")
                .maxAgeVideoFiles("20d")
                .buildAsList();

        List<String> expected = List.of(
                "rclone",
                "-v",
                "--config=/tmp/rclone.conf",
                "delete",
                "remote:records",
                "--min-age",
                "20d"
        );

        assertEquals(expected, command);
    }

    @Test
    void buildsCommandStringJoinedBySpaces() {
        String command = RcloneDeleteCommandParser.builder()
                .configLocation("/tmp/rclone.conf")
                .folder("remote:records")
                .maxAgeVideoFiles("48h")
                .build();

        assertEquals(
                "rclone -v --config=/tmp/rclone.conf delete remote:records --min-age 48h",
                command);
    }
}
