package com.icaroerasmo.runners;

import com.icaroerasmo.parsers.RcloneRmdirsCommandParser;
import com.icaroerasmo.properties.RcloneProperties;
import com.icaroerasmo.properties.TelegramProperties;
import com.icaroerasmo.storage.FutureStorage;
import com.icaroerasmo.util.Utilities;
import com.pengrad.telegrambot.TelegramBot;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

@Log4j2
@Component
public class RcloneRmdirsRunner extends RcloneRunner {

    private final RcloneProperties rcloneProperties;

    public RcloneRmdirsRunner(
            ExecutorService executorService,
            FutureStorage futureStorage,
            RcloneProperties rcloneProperties,
            TelegramProperties telegramProperties,
            TelegramBot telegram,
            TranslateShellRunner translateShellRunner,
            Utilities utilities) {
        super(executorService, futureStorage, telegramProperties, telegram, translateShellRunner, utilities);
        this.rcloneProperties = rcloneProperties;
    }

    public Void run() {
        log.info("Running rclone");

        final RcloneRmdirsCommandParser.RcloneRmdirsCommandParserBuilder command = RcloneRmdirsCommandParser.builder()
                .configLocation(rcloneProperties.getConfigLocation())
                .folder(rcloneProperties.getDestinationFolder());

        log.info("Rclone command: {}", command.build());

        start(command);

        log.info("Rclone finished.");

        return null;
    }
}
