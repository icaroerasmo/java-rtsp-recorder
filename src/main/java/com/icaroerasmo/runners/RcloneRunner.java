package com.icaroerasmo.runners;

import com.icaroerasmo.enums.MessagesEnum;
import com.icaroerasmo.messaging.NotificationPublisher;
import com.icaroerasmo.parsers.CommandParser;
import com.icaroerasmo.storage.FutureStorage;
import com.icaroerasmo.util.Utilities;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.util.Strings;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Log4j2
public abstract class RcloneRunner extends AbstractRunner implements IRcloneRunner {

    private final FutureStorage futureStorage;
    private final NotificationPublisher publisher;
    private final Utilities utilities;

    public RcloneRunner(
            ExecutorService executorService,
            FutureStorage futureStorage,
            NotificationPublisher publisher,
            Utilities utilities
    ) {
        super(executorService);
        this.futureStorage = futureStorage;
        this.publisher = publisher;
        this.utilities = utilities;
    }

    @SneakyThrows
    public void start(CommandParser.CommandParserBuilder command) {
        Process process = null;
        Future<StringBuilder> outputLogsFuture;
        Future<StringBuilder> errorLogsFuture = null;

        final List<String> commandList = command.buildAsList();

        sendStartNotification(startProcessMessagePicker(commandList));

        MessagesEnum message = successMessagePicker(commandList);

        try {

            process = new ProcessBuilder(commandList).start();

            outputLogsFuture = launchLogListener(process.getInputStream(), "Rclone", "Stream closed for rclone output logs thread.");

            errorLogsFuture = launchLogListener(process.getErrorStream(), "Rclone", "Stream closed for rclone error logs thread.");

            futureStorage.put("rclone", commandList.get(1) + " outputLogsFuture", outputLogsFuture);
            futureStorage.put("rclone", commandList.get(1) + " errorLogsFuture", errorLogsFuture);

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new RuntimeException("Rclone: execution failed with exit code " + exitCode);
            }

        } catch (Exception e) {

            if(e instanceof InterruptedException) {
                log.warn("Rclone: Interrupted.");
                Thread.currentThread().interrupt();
            } else {
                log.error("Rclone: Unexpected error occurred.");
                log.error("Rclone: {}", e.getMessage());
            }

            message = errorMessagePicker(commandList);

        } finally {

            utilities.killProcess(process);

            final StringBuilder outputLogs = errorLogsFuture != null ? errorLogsFuture.get() : null;

            sendEndNotification(outputLogs, message);
        }
    }


    @Override
    public void sendStartNotification(MessagesEnum message) {
        publisher.publishText(message, formattedDateForCaption(LocalDateTime.now()));
    }

    @Override
    public void sendEndNotification(StringBuilder outputLogs, MessagesEnum messagesEnum) {
        if(outputLogs == null || Strings.isBlank(outputLogs.toString())) {
            publisher.publishNoLogs(messagesEnum, formattedDateForCaption(LocalDateTime.now()));
        } else {
            publisher.publishDocument(messagesEnum,
                    "log%s.log".formatted(formattedDateForLogName(LocalDateTime.now())),
                    outputLogs.toString().getBytes(StandardCharsets.UTF_8),
                    formattedDateForCaption(LocalDateTime.now()));
        }
    }

    private MessagesEnum startProcessMessagePicker(List<String> command) {
        if(command.contains("delete")) {
            return MessagesEnum.RCLONE_DELETE_START;
        } else if(command.contains("rmdirs")) {
            return MessagesEnum.RCLONE_RMDIRS_START;
        } else if(command.contains("dedupe")) {
            return MessagesEnum.RCLONE_DEDUPE_START;
        } else {
            return MessagesEnum.RCLONE_SYNC_START;
        }
    }

    private MessagesEnum successMessagePicker(List<String> command) {
        if(command.contains("delete")) {
            return MessagesEnum.RCLONE_DELETE_SUCCESS;
        } else if(command.contains("rmdirs")) {
            return MessagesEnum.RCLONE_RMDIRS_SUCCESS;
        } else if(command.contains("dedupe")) {
            return MessagesEnum.RCLONE_DEDUPE_SUCCESS;
        } else {
            return MessagesEnum.RCLONE_SYNC_SUCCESS;
        }
    }

    private MessagesEnum errorMessagePicker(List<String> command) {
        if(command.contains("delete")) {
            return MessagesEnum.RCLONE_DELETE_ERROR;
        } else if(command.contains("rmdirs")) {
            return MessagesEnum.RCLONE_RMDIRS_ERROR;
        } else if(command.contains("dedupe")) {
            return MessagesEnum.RCLONE_DEDUPE_ERROR;
        } else {
            return MessagesEnum.RCLONE_SYNC_ERROR;
        }
    }
}
