package com.icaroerasmo.runners;

import com.icaroerasmo.parsers.RcloneSyncCommandParser;
import com.icaroerasmo.properties.RcloneProperties;
import com.icaroerasmo.properties.StorageProperties;
import com.icaroerasmo.properties.TelegramProperties;
import com.icaroerasmo.storage.FutureStorage;
import com.icaroerasmo.util.Utilities;
import com.pengrad.telegrambot.TelegramBot;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

@Log4j2
@Component
public class RcloneSyncRunner extends RcloneRunner {

    private final RcloneProperties rcloneProperties;
    private final StorageProperties storageProperties;

    public RcloneSyncRunner(
            ExecutorService executorService,
            FutureStorage futureStorage,
            RcloneProperties rcloneProperties,
            TelegramProperties telegramProperties,
            StorageProperties storageProperties,
            TelegramBot telegram,
            TranslateShellRunner translateShellRunner,
            Utilities utilities) {
        super(executorService, futureStorage, telegramProperties, telegram, translateShellRunner, utilities);
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
