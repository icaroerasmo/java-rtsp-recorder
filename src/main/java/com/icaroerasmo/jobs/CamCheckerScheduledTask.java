package com.icaroerasmo.jobs;

import com.icaroerasmo.enums.MessagesEnum;
import com.icaroerasmo.properties.RtspProperties;
import com.icaroerasmo.properties.TelegramProperties;
import com.icaroerasmo.runners.TranslateShellRunner;
import com.icaroerasmo.services.FfmpegService;
import com.icaroerasmo.storage.FutureStorage;
import com.icaroerasmo.util.TelegramUtil;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.Future;

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
        for(RtspProperties.Camera camera : rtspProperties.getCameras()) {
            // Check if camera is online
            // If camera is online, do nothing
            // If camera is offline, send a message to Telegram
            final boolean isRunning = futureStorage.get(
                    camera.getName(), "main").
                    state().equals(Future.State.RUNNING);
            if(!isRunning) {
                // Send message to Telegram
                telegramUtil.sendMessage(MessagesEnum.CAM_CHECKER_NOT_RUNNING, camera.getName());
                futureStorage.delete(camera.getName());

                // Restart camera
                ffmpegService.start(camera.getName());
            }
        }
    }
}
