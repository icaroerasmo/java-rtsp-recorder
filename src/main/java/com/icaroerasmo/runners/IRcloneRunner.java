package com.icaroerasmo.runners;

import com.icaroerasmo.enums.MessagesEnum;
import com.icaroerasmo.parsers.CommandParser;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public interface IRcloneRunner {

    Void run();
    void start(CommandParser.CommandParserBuilder command);
    void sendStartNotification(MessagesEnum messagesEnum);
    void sendEndNotification(StringBuilder outputLogs, MessagesEnum messagesEnum);

    default String formattedDateForCaption(LocalDateTime time) {
        final String timeFormatPattern = " 'at' HH:mm:ss";
        return dateTimeFormatter(timeFormatPattern, time);
    }

    default String formattedDateForLogName(LocalDateTime time) {
        final String timeFormatPattern = "_HH-mm-ss";
        return dateTimeFormatter(timeFormatPattern, time).replace("/", "-");
    }

    default String dateTimeFormatter(String timeFormatPattern, LocalDateTime time) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(timeFormatPattern);
        return time.format(dateFormatter) + time.format(timeFormatter);
    }
}
