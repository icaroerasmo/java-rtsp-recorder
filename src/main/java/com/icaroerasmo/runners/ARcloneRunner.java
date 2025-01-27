package com.icaroerasmo.runners;

import com.icaroerasmo.enums.MessagesEnum;
import com.icaroerasmo.parsers.CommandParser;
import com.icaroerasmo.properties.TelegramProperties;
import com.icaroerasmo.storage.FutureStorage;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.SendDocument;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.util.Strings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Log4j2
@RequiredArgsConstructor
public abstract class ARcloneRunner implements IRcloneRunner {

    private final ExecutorService executorService;
    private final FutureStorage futureStorage;
    private final TelegramProperties telegramProperties;
    private final TelegramBot telegram;
    private final TranslateShellRunner translateShellRunner;

    @SneakyThrows
    public void start(CommandParser.CommandParserBuilder command) {
        Process process = null;
        Future<StringBuilder> outputLogsFuture = null;
        Future<StringBuilder> errorLogsFuture = null;

        final List<String> commandList = command.buildAsList();

        MessagesEnum startMessage = startProcessMessagePicker(commandList.get(1));
        MessagesEnum failedMessage = successMessagePicker(commandList.get(1));

        sendStartNotification(startMessage);

        try {

            process = new ProcessBuilder(commandList).start();

            final Process finalProcess = process;

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

            failedMessage = errorMessagePicker(commandList.get(1));

        } finally {

            if (process != null) {
                process.destroy();
            }

            final StringBuilder outputLogs = errorLogsFuture != null ? errorLogsFuture.get() : null;

            sendEndNotification(outputLogs, failedMessage);
        }
    }

    @Override
    public void sendStartNotification(MessagesEnum message) {
        final SendMessage request = new SendMessage(telegramProperties.getChatId(),
                translateShellRunner.translateText(message.getMessage().
                        formatted(formattedDateForCaption(LocalDateTime.now()))));
        telegram.execute(request);
    }

    @Override
    public void sendEndNotification(StringBuilder outputLogs, MessagesEnum messagesEnum) {
        BaseRequest<?, ?> request = null;

        if(outputLogs == null || Strings.isBlank(outputLogs.toString())) {
            request = new SendMessage(telegramProperties.getChatId(),
                    translateShellRunner.translateText(
                            (messagesEnum.getMessage()+". "+MessagesEnum.RCLONE_NO_LOGS.getMessage()).
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

    private MessagesEnum startProcessMessagePicker(String command) {
        return switch (command) {
            case "delete" -> MessagesEnum.RCLONE_DELETE_START;
            case "rmdirs" -> MessagesEnum.RCLONE_RMDIRS_START;
            case "dedupe" -> MessagesEnum.RCLONE_DEDUPE_START;
            default -> MessagesEnum.RCLONE_SYNC_START;
        };
    }

    private MessagesEnum successMessagePicker(String command) {
        return switch (command) {
            case "delete" -> MessagesEnum.RCLONE_DELETE_SUCCESS;
            case "rmdirs" -> MessagesEnum.RCLONE_RMDIRS_SUCCESS;
            case "dedupe" -> MessagesEnum.RCLONE_DEDUPE_SUCCESS;
            default -> MessagesEnum.RCLONE_SYNC_SUCCESS;
        };
    }

    private MessagesEnum errorMessagePicker(String command) {
        return switch (command) {
            case "delete" -> MessagesEnum.RCLONE_DELETE_ERROR;
            case "rmdirs" -> MessagesEnum.RCLONE_RMDIRS_ERROR;
            case "dedupe" -> MessagesEnum.RCLONE_DEDUPE_ERROR;
            default -> MessagesEnum.RCLONE_SYNC_ERROR;
        };
    }
}
