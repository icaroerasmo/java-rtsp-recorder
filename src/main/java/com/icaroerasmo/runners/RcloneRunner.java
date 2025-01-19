package com.icaroerasmo.runners;

import com.icaroerasmo.properties.ConfigYaml;
import com.icaroerasmo.properties.RcloneProperties;
import com.icaroerasmo.properties.TelegramProperties;
import com.icaroerasmo.storage.FutureStorage;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendDocument;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final TelegramBot telegram;

    public Void run() {
        log.info("Running rclone");

        final String command = "rclone " + rcloneProperties.getParameters();

        Process process = null;
        Future<Void> outputLogsFuture = null;
        Future<Void> errorLogsFuture = null;
        try {
            process = new ProcessBuilder(command.split(" ")).start();

            Process finalProcess = process;

            outputLogsFuture = executorService.submit(() -> {

                sendSynchronizationStartedNotification();

                final StringBuilder outputLogs = new StringBuilder();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(finalProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.debug("Rclone: {}", line);
                        outputLogs.append(line).append("\n");
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Rclone: Error reading output.", e);
                }

                sendSynchronizationEndedNotification(outputLogs);

                return null;
            });

            errorLogsFuture = executorService.submit(() -> {

                StringBuilder errorLogs = new StringBuilder();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(finalProcess.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.debug("Rclone: {}", line);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Rclone: Error reading error output", e);
                }

                if(!errorLogs.isEmpty()) {
                    sendSynchronizationErrorNotification(errorLogs);
                }

                return null;
            });

            futureStorage.put("rclone", "outputLogsFuture", outputLogsFuture);
            futureStorage.put("rclone", "errorLogsFuture", errorLogsFuture);

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new RuntimeException("Rclone: execution failed with exit code " + exitCode);
            }

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
        }

        log.info("Rclone finished.");

        return null;
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
