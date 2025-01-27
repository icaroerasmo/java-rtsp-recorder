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
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;

@Log4j2
@Component
public class RcloneSyncRunner extends ARcloneRunner {

    private final RcloneProperties rcloneProperties;
    private final StorageProperties storageProperties;

    @Autowired
    public RcloneSyncRunner(
            ExecutorService executorService,
            FutureStorage futureStorage,
            RcloneProperties rcloneProperties,
            TelegramProperties telegramProperties,
            StorageProperties storageProperties,
            TelegramBot telegram,
            TranslateShellRunner translateShellRunner) {
        super(executorService, futureStorage, telegramProperties, telegram, translateShellRunner);
        this.rcloneProperties = rcloneProperties;
        this.storageProperties = storageProperties;
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

        start(command);

        log.info("Rclone finished.");

        return null;
    }
}
