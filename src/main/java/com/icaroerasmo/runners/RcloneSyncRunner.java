package com.icaroerasmo.runners;

import com.icaroerasmo.enums.MessagesEnum;
import com.icaroerasmo.parsers.RcloneSyncCommandParser;
import com.icaroerasmo.properties.RcloneProperties;
import com.icaroerasmo.properties.StorageProperties;
import com.icaroerasmo.properties.TelegramProperties;
import com.icaroerasmo.storage.FutureStorage;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.SendDocument;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Log4j2
@Component
public class RcloneSyncRunner extends ARcloneRunner {

    private final RcloneProperties rcloneProperties;
    private final TelegramProperties telegramProperties;
    private final StorageProperties storageProperties;
    private final TelegramBot telegram;
    private final TranslateShellRunner translateShellRunner;

    @Autowired
    public RcloneSyncRunner(
            ExecutorService executorService,
            FutureStorage futureStorage,
            RcloneProperties rcloneProperties,
            TelegramProperties telegramProperties,
            StorageProperties storageProperties,
            TelegramBot telegram,
            TranslateShellRunner translateShellRunner) {
        super(executorService, futureStorage);
        this.rcloneProperties = rcloneProperties;
        this.telegramProperties = telegramProperties;
        this.storageProperties = storageProperties;
        this.telegram = telegram;
        this.translateShellRunner = translateShellRunner;
    }

    public Void run() {
        log.info("Running rclone");

        final RcloneSyncCommandParser.RcloneSyncCommandParserBuilder command = RcloneSyncCommandParser.builder()
                .configLocation(rcloneProperties.getConfigLocation())
                .transferMethod(rcloneProperties.getTransferMethod())
                .sourceFolder(storageProperties.getRecordsFolder())
                .destinationFolder(rcloneProperties.getDestinationFolder())
                .excludePatterns(rcloneProperties.getExcludePatterns())
                .ignoreExisting(rcloneProperties.isIgnoreExisting());

        log.info("Rclone command: {}", command.build());

        MessagesEnum message = MessagesEnum.RCLONE_SUCCESS;

        sendStartNotification();

        start(command, message);

        log.info("Rclone finished.");

        return null;
    }

    @Override
    public void sendStartNotification() {
        final SendMessage request = new SendMessage(telegramProperties.getChatId(),
                translateShellRunner.translateText("Synchronization started in %s successfully.".
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
}
