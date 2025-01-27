package com.icaroerasmo.runners;

import com.icaroerasmo.parsers.RcloneRmdirsCommandParser;
import com.icaroerasmo.properties.RcloneProperties;
import com.icaroerasmo.properties.TelegramProperties;
import com.icaroerasmo.storage.FutureStorage;
import com.pengrad.telegrambot.TelegramBot;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

@Log4j2
@Component
public class RcloneDedupeRunner extends ARcloneRunner {

    private final RcloneProperties rcloneProperties;

    @Autowired
    public RcloneDedupeRunner(
            ExecutorService executorService,
            FutureStorage futureStorage,
            RcloneProperties rcloneProperties,
            TelegramProperties telegramProperties,
            TelegramBot telegram,
            TranslateShellRunner translateShellRunner) {
        super(executorService, futureStorage, telegramProperties, telegram, translateShellRunner);
        this.rcloneProperties = rcloneProperties;
    }

    public Void run() {
        log.info("Running rclone");

        final RcloneRmdirsCommandParser.RcloneRmdirsCommandParserBuilder command = RcloneRmdirsCommandParser.builder()
                .folder(rcloneProperties.getDestinationFolder());

        log.info("Rclone command: {}", command.build());

        start(command);

        log.info("Rclone finished.");

        return null;
    }
}
