package com.icaroerasmo.util;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

@Component
public class DateExtractor {

    public Map<String, String> extractDate(String input) {

        Map<String, String> dateMap = new HashMap<>();

        String regex = ".*?(.+)(\\d{4})-(\\d{2})-(\\d{2})_(\\d{2})-\\d{2}-\\d{2}\\.mkv";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        if (matcher.matches()) {
            String camName = matcher.group(1);
            int year = Integer.parseInt(matcher.group(2));
            int month = Integer.parseInt(matcher.group(3));
            int day = Integer.parseInt(matcher.group(4));
            int hour = Integer.parseInt(matcher.group(5));

            LocalDate date = LocalDate.of(year, month, day);
            String monthName = date.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);

            dateMap.put("camName", camName);
            dateMap.put("year", String.valueOf(year));
            dateMap.put("month", monthName);
            dateMap.put("day", String.valueOf(day));
            dateMap.put("hour", String.valueOf(hour));
        } else {
            throw new IllegalArgumentException("Input string does not match the expected pattern.");
        }

        return dateMap;
    }
}