package com.icaroerasmo.runners;

import com.icaroerasmo.enums.MessagesEnum;
import com.icaroerasmo.parsers.RcloneCommandParser;
import com.icaroerasmo.properties.ConfigYaml;
import com.icaroerasmo.properties.RcloneProperties;
import com.icaroerasmo.properties.StorageProperties;
import com.icaroerasmo.properties.TelegramProperties;
import com.icaroerasmo.storage.FutureStorage;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.SendDocument;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
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
    private final TranslateShellRunner translateShellRunner;

    @SneakyThrows
    public Void run() {
        log.info("Running rclone");

        final RcloneCommandParser.RcloneCommandParserBuilder command = RcloneCommandParser.builder()
                .configLocation(rcloneProperties.getConfigLocation())
                .transferMethod(rcloneProperties.getTransferMethod())
                .sourceFolder(storageProperties.getRecordsFolder())
                .destinationFolder(rcloneProperties.getDestinationFolder())
                .excludePatterns(rcloneProperties.getExcludePatterns())
                .ignoreExisting(rcloneProperties.isIgnoreExisting());

        log.info("Rclone command: {}", command.build());

        MessagesEnum message = MessagesEnum.RCLONE_SUCCESS;
        Process process = null;
        Future<StringBuilder> outputLogsFuture = null;
        Future<StringBuilder> errorLogsFuture = null;

        sendSynchronizationStartedNotification();

        try {

            process = new ProcessBuilder(command.buildAsList()).start();

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
                        log.info("Rclone: {}", line);
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

            message = MessagesEnum.RCLONE_ERROR;

        } finally {

            if (process != null) {
                process.destroy();
            }

            final StringBuilder outputLogs = errorLogsFuture != null ? errorLogsFuture.get() : null;

            sendSynchronizationEndedNotification(outputLogs, message);
        }

        log.info("Rclone finished.");

        return null;
    }

    private void sendSynchronizationEndedNotification(StringBuilder outputLogs, MessagesEnum messagesEnum) {

        BaseRequest<?, ?> request = null;

        if(outputLogs == null || Strings.isBlank(outputLogs.toString())) {
            request = new SendMessage(telegramProperties.getChatId(),
                    translateShellRunner.translateText(
                            messagesEnum.getMessage()+". "+
                            MessagesEnum.RCLONE_NO_LOGS.getMessage().
                            formatted(formattedDateForCaption(LocalDateTime.now()))));
        } else {
            request = new SendDocument(telegramProperties.getChatId(),
                    outputLogs.toString().getBytes(StandardCharsets.UTF_8)).
                    fileName("log%s.log".formatted(formattedDateForLogName(LocalDateTime.now()))).
                    caption(
                            translateShellRunner.translateText(messagesEnum.getMessage().formatted(
                                    formattedDateForCaption(LocalDateTime.now()))));
        }

        telegram.execute(request);
    }

    private void sendSynchronizationStartedNotification() {
        final SendMessage request = new SendMessage(telegramProperties.getChatId(),
                translateShellRunner.translateText("Synchronization started in %s successfully.".
                        formatted(formattedDateForCaption(LocalDateTime.now()))));
        telegram.execute(request);
    }

    private String formattedDateForCaption(LocalDateTime time) {
        final String timeFormatPattern = " 'at' HH:mm:ss";
        return dateTimeFormatter(timeFormatPattern, time);
    }

    private String formattedDateForLogName(LocalDateTime time) {
        final String timeFormatPattern = "_HH-mm-ss";
        return dateTimeFormatter(timeFormatPattern, time).replace("/", "-");
    }

    private static String dateTimeFormatter(String timeFormatPattern, LocalDateTime time) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(timeFormatPattern);
        return time.format(dateFormatter) + time.format(timeFormatter);
    }
}
