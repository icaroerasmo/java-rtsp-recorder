package com.icaroerasmo.jobs;

import com.icaroerasmo.enums.MessagesEnum;
import com.icaroerasmo.properties.RtspProperties;
import com.icaroerasmo.services.FfmpegService;
import com.icaroerasmo.storage.FutureStorage;
import com.icaroerasmo.util.TelegramUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class CamCheckerScheduledTask {

    private final TelegramUtil telegramUtil;
    private final RtspProperties rtspProperties;
    private final FutureStorage futureStorage;
    private final FfmpegService ffmpegService;

    @SneakyThrows
    @Scheduled(fixedDelay = 60000)
    public void checkIfCamsAreOnline() {
        rtspProperties.getCameras().stream().parallel().
                forEach(camera -> {
                // Check if camera is online
                // If camera is online, do nothing
                // If camera is offline, send a message to Telegram

                if(futureStorage.isRunning(camera.getName())) {
                    return;
                }

                log.warn("Cam {} is not running...", camera.getName());

                // Send message to Telegram
                telegramUtil.sendMessage(MessagesEnum.CAM_CHECKER_NOT_RUNNING, camera.getName());
                futureStorage.delete(camera.getName());

                // Restart camera
                ffmpegService.start(camera.getName());
        });
    }
}
