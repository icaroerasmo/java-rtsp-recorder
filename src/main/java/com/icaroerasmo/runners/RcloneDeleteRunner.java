package com.icaroerasmo.runners;

import com.icaroerasmo.parsers.RcloneDeleteCommandParser;
import com.icaroerasmo.properties.RcloneProperties;
import com.icaroerasmo.properties.StorageProperties;
import com.icaroerasmo.properties.TelegramProperties;
import com.icaroerasmo.storage.FutureStorage;
import com.pengrad.telegrambot.TelegramBot;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

@Log4j2
@Component
public class RcloneDeleteRunner extends RcloneRunner {

    private final StorageProperties storageProperties;
    private final RcloneProperties rcloneProperties;

    @Autowired
    public RcloneDeleteRunner(
            ExecutorService executorService,
            FutureStorage futureStorage,
            RcloneProperties rcloneProperties,
            TelegramProperties telegramProperties,
            TelegramBot telegram,
            TranslateShellRunner translateShellRunner, StorageProperties storageProperties) {
        super(executorService, futureStorage, telegramProperties, telegram, translateShellRunner);
        this.storageProperties = storageProperties;
        this.rcloneProperties = rcloneProperties;
    }

    public Void run() {
        log.info("Running rclone");

        final RcloneDeleteCommandParser.RcloneDeleteCommandParserBuilder command = RcloneDeleteCommandParser.builder()
                .maxAgeVideoFiles(storageProperties.getMaxAgeVideoFiles())
                .configLocation(rcloneProperties.getConfigLocation())
                .folder(rcloneProperties.getDestinationFolder());

        log.info("Rclone command: {}", command.build());

        start(command);

        log.info("Rclone finished.");

        return null;
    }
}
