package com.icaroerasmo.runners;

import com.icaroerasmo.parsers.RcloneDedupeCommandParser;
import com.icaroerasmo.properties.RcloneProperties;
import com.icaroerasmo.properties.TelegramProperties;
import com.icaroerasmo.storage.FutureStorage;
import com.icaroerasmo.util.TelegramUtil;
import com.icaroerasmo.util.Utilities;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

@Log4j2
@Component
public class RcloneDedupeRunner extends RcloneRunner {

    private final RcloneProperties rcloneProperties;

    public RcloneDedupeRunner(
            ExecutorService executorService,
            FutureStorage futureStorage,
            RcloneProperties rcloneProperties,
            TelegramProperties telegramProperties,
            TelegramUtil telegramUtil,
            Utilities utilities) {
        super(executorService, futureStorage, telegramProperties, telegramUtil, utilities);
        this.rcloneProperties = rcloneProperties;
    }

    public Void run() {
        log.info("Running rclone");

        final RcloneDedupeCommandParser.RcloneDedupeCommandParserBuilder command = RcloneDedupeCommandParser.builder()
                .configLocation(rcloneProperties.getConfigLocation())
                .folder(rcloneProperties.getDestinationFolder());

        log.info("Rclone command: {}", command.build());

        start(command);

        log.info("Rclone finished.");

        return null;
    }
}
