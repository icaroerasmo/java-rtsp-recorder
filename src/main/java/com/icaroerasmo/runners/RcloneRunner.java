package com.icaroerasmo.runners;

import com.icaroerasmo.enums.MessagesEnum;
import com.icaroerasmo.parsers.CommandParser;
import com.icaroerasmo.properties.TelegramProperties;
import com.icaroerasmo.storage.FutureStorage;
import com.icaroerasmo.util.Utilities;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.SendDocument;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Log4j2
public abstract class RcloneRunner extends AbstractRunner implements IRcloneRunner {

    private final FutureStorage futureStorage;
    private final TelegramProperties telegramProperties;
    private final TelegramBot telegram;
    private final TranslateShellRunner translateShellRunner;
    private final Utilities utilities;

    public RcloneRunner(
            ExecutorService executorService,
            FutureStorage futureStorage,
            TelegramProperties telegramProperties,
            TelegramBot telegram,
            TranslateShellRunner translateShellRunner,
            Utilities utilities
            ) {
        super(executorService);
        this.futureStorage = futureStorage;
        this.telegramProperties = telegramProperties;
        this.telegram = telegram;
        this.translateShellRunner = translateShellRunner;
        this.utilities = utilities;
    }

    @SneakyThrows
    public void start(CommandParser.CommandParserBuilder command) {
        Process process = null;
        Future<StringBuilder> outputLogsFuture = null;
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
