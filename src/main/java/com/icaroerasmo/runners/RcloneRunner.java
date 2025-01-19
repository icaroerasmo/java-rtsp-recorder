package com.icaroerasmo.runners;

import com.icaroerasmo.parsers.RcloneCommandParser;
import com.icaroerasmo.properties.ConfigYaml;
import com.icaroerasmo.properties.RcloneProperties;
import com.icaroerasmo.properties.StorageProperties;
import com.icaroerasmo.properties.TelegramProperties;
import com.icaroerasmo.storage.FutureStorage;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendDocument;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Log4j2
@Getter
@Component
@RequiredArgsConstructor
public class RcloneRunner implements ConfigYaml {

    private final ExecutorService executorService;
    private final FutureStorage futureStorage;
    private final RcloneProperties rcloneProperties;
    private final TelegramProperties telegramProperties;
    private final StorageProperties storageProperties;
    private final TelegramBot telegram;

    public Void run() {
        log.info("Running rclone");

        final List<String> command = RcloneCommandParser.builder()
                .transferMethod(rcloneProperties.getTransferMethod())
                .sourceFolder(storageProperties.getRecordsFolder())
                .destinationFolder(rcloneProperties.getDestinationFolder())
                .excludePatterns(rcloneProperties.getExcludePatterns())
                .ignoreExisting(rcloneProperties.isIgnoreExisting())
                .buildAsList();

        Process process = null;
        Future<StringBuilder> outputLogsFuture = null;
        Future<StringBuilder> errorLogsFuture = null;

        sendSynchronizationStartedNotification();

        try {

            process = new ProcessBuilder(command).start();

            Process finalProcess = process;

            outputLogsFuture = executorService.submit(() -> {

                final StringBuilder outputLogs = new StringBuilder();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(finalProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info("Rclone: {}", line);
                        outputLogs.append(line).append("\n");
                    }
                } catch (IOException e) {
                    log.debug("Stream closed for rclone output logs thread.");
                }

                return outputLogs;
            });

            errorLogsFuture = executorService.submit(() -> {

                final StringBuilder errorLogs = new StringBuilder();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(finalProcess.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.error("Rclone: {}", line);
                        errorLogs.append(line).append("\n");
                    }
                } catch (IOException e) {
                    log.debug("Stream closed for rclone error logs thread.");
                }

                return errorLogs;
            });

            futureStorage.put("rclone", "outputLogsFuture", outputLogsFuture);
            futureStorage.put("rclone", "errorLogsFuture", errorLogsFuture);

            int exitCode = process.waitFor();

            StringBuilder outputLogs = outputLogsFuture.get();

            if (exitCode != 0) {
                throw new RuntimeException("Rclone: execution failed with exit code " + exitCode);
            }

            sendSynchronizationEndedNotification(outputLogs);

        } catch (InterruptedException e) {
            log.warn("Rclone: Interrupted.");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Rclone: Unexpected error occurred.");
            log.error("Rclone: {}", e.getMessage());
        } finally {
            if (process != null) {
                process.destroy();
            }

            StringBuilder errorLogs = null;
            try {
                errorLogs = errorLogsFuture.get();
            } catch (Exception e) {}

            if(!errorLogs.isEmpty()) {
                sendSynchronizationErrorNotification(errorLogs);
            }
        }

        log.info("Rclone finished.");

        return null;
    }

    public String buildRcloneCommand() {
        StringBuilder command = new StringBuilder("rclone -vv ");

        // Add transfer method
        command.append(rcloneProperties.getTransferMethod()).append(" ");

        // Add source and destination folders
        command.append("/home/icaroerasmo/rtsp-test/records ").append(rcloneProperties.getDestinationFolder()).append(" ");

        // Add exclude patterns
        for (String pattern : rcloneProperties.getExcludePatterns()) {
            command.append("--exclude=").append(pattern).append(" ");
        }

        // Add ignore existing flag
        if (rcloneProperties.isIgnoreExisting()) {
            command.append("--ignore-existing ");
        }

        return command.toString().trim();
    }

    private void sendSynchronizationEndedNotification(StringBuilder outputLogs) {
        SendDocument request = new SendDocument(telegramProperties.getChatId(), outputLogs.toString());
        request.fileName("log%s.log".formatted(formattedDateForLogName(LocalDateTime.now())));
        request.caption("Synchroniation ended %s successfully.".formatted(formattedDateForCaption(LocalDateTime.now())));

        telegram.execute(request);
    }

    private void sendSynchronizationErrorNotification(StringBuilder errorLogs) {
        SendDocument request = new SendDocument(telegramProperties.getChatId(), errorLogs.toString());
        request.fileName("log%s.log".formatted(formattedDateForLogName(LocalDateTime.now())));
        request.caption("Synchroniation failed in %s.".formatted(formattedDateForCaption(LocalDateTime.now())));

        telegram.execute(request);
    }

    private void sendSynchronizationStartedNotification() {
        final SendMessage request = new SendMessage(telegramProperties.getChatId(),
                "Synchroniation started %s successfully.".
                        formatted(formattedDateForCaption(LocalDateTime.now())));
        telegram.execute(request);
    }

    private String formattedDateForCaption(LocalDateTime time) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'at' HH:mm");
        return time.format(formatter);
    }

    private String formattedDateForLogName(LocalDateTime time) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy_HH-mm");
        return time.format(formatter);
    }
}
