package com.icaroerasmo.parsers;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RcloneSyncCommandParserTest {

    @Test
    void buildsSyncCommandWithAllOptions() {
        List<String> command = RcloneSyncCommandParser.builder()
                .configLocation("/tmp/rclone.conf")
                .transferMethod("copy")
                .sourceFolder("/app/data/records")
                .destinationFolder("remote:records")
                .excludePatterns(List.of("*.tmp", "*.part"))
                .ignoreExisting(true)
                .buildAsList();

        List<String> expected = List.of(
                "rclone",
                "-v",
                "--config=/tmp/rclone.conf",
                "copy",
                "/app/data/records",
                "remote:records",
                "--exclude=*.tmp",
                "--exclude=*.part",
                "--ignore-existing"
        );

        assertEquals(expected, command);
    }

    @Test
    void omitsIgnoreExistingFlagWhenFalse() {
        List<String> command = RcloneSyncCommandParser.builder()
                .configLocation("/tmp/rclone.conf")
                .transferMethod("sync")
                .sourceFolder("/src")
                .destinationFolder("remote:/dst")
                .excludePatterns(List.of())
                .ignoreExisting(false)
                .buildAsList();

        assertEquals(List.of(
                "rclone", "-v", "--config=/tmp/rclone.conf",
                "sync", "/src", "remote:/dst"), command);
    }

    @Test
    void buildsCommandStringJoinedBySpaces() {
        String command = RcloneSyncCommandParser.builder()
                .configLocation("/tmp/rclone.conf")
                .transferMethod("copy")
                .sourceFolder("/src")
                .destinationFolder("remote:/dst")
                .excludePatterns(List.of("*.tmp"))
                .ignoreExisting(true)
                .build();

        assertEquals(
                "rclone -v --config=/tmp/rclone.conf copy /src remote:/dst --exclude=*.tmp --ignore-existing",
                command);
    }
}
