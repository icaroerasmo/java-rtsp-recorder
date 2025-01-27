package com.icaroerasmo.parsers;

import java.util.List;

public interface CommandParser {
    interface CommandParserBuilder {
        List<String> buildAsList();
        default String build() {
            return String.join(" ", buildAsList());
        }
    }
}
