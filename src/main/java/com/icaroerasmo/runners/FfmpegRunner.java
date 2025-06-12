package com.icaroerasmo.runners;

import com.icaroerasmo.enums.MessagesEnum;
import com.icaroerasmo.parsers.FfmpegCommandParser;
import com.icaroerasmo.storage.FutureStorage;
import com.icaroerasmo.util.TelegramUtil;
import com.icaroerasmo.util.Utilities;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Log4j2
@Getter
@Component
@RequiredArgsConstructor
public class FfmpegRunner {

    private final ExecutorService executorService;
    private final FutureStorage futureStorage;
    private final TelegramUtil telegramUtil;
    private final Utilities utilities;

    @SneakyThrows
    public Void run(String camName, FfmpegCommandParser.FfmpegCommandParserBuilder command) {

        log.info("Cam {}: Running command: {}", camName, command.build());

        final int maxRetries = 3;
        int attempt = 0;
        boolean success = false;

            while (attempt < maxRetries && !success) {
                Process process = null;
                Future<Void> outputLogsFuture = null;
                Future<Void> errorLogsFuture = null;
                try {
                    process = new ProcessBuilder(command.buildAsList()).start();

                    Process finalProcess = process;

                    outputLogsFuture = executorService.submit(() -> {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(finalProcess.getInputStream()))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                log.debug("Cam {}: {}", camName, line);
                            }
                        } catch (IOException e) {
                            throw new RuntimeException("Cam " + camName + ": Error reading ffmpeg output", e);
                        }
                        return null;
                    });

                    errorLogsFuture = executorService.submit(() -> {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(finalProcess.getErrorStream()))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                log.debug("Cam {}: {}", camName, line);
                            }
                        } catch (IOException e) {
                            throw new RuntimeException("Cam " + camName + ": Error reading ffmpeg error output", e);
                        }
                        return null;
                    });

                    futureStorage.put(camName, "outputLogsFuture", outputLogsFuture);
                    futureStorage.put(camName, "errorLogsFuture", errorLogsFuture);

                    log.info("Cam {}: ffmpeg started.", camName);
                    telegramUtil.sendMessage(MessagesEnum.CAM_STARTED, camName);

                    int exitCode = process.waitFor();

                    if (exitCode == 0) {
                        attempt = 0;
                        success = true;
                    } else {
                        throw new RuntimeException("Cam " + camName + ": ffmpeg execution failed with exit code " + exitCode);
                    }
                } catch (InterruptedException e) {
                    telegramUtil.sendMessage(MessagesEnum.CAM_STOPPED, camName);
                    log.warn("Cam {}: Interrupted.", camName);
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    int attemptNo = attempt + 1;
                    log.warn("Cam {}: Starting attempt number {} failed.", camName, attemptNo, e);
                    telegramUtil.sendMessage(MessagesEnum.CAM_ATTEMPT_FAILED, camName, attemptNo);
                } finally {
                    utilities.killProcess(process);
                }

                attempt++;

                if (!success && attempt >= maxRetries) {
                    attempt = 0;
                    log.error("Cam {}: ffmpeg execution failed after " + maxRetries + " attempts. Retrying in 5 minutes...", camName);
                    telegramUtil.sendMessage(MessagesEnum.CAM_MAX_ATTEMPTS_REACHED, camName, maxRetries);
                    Thread.sleep(300000);
                    log.info("Cam {}: Trying to run again after hibernation.", camName);
                    telegramUtil.sendMessage(MessagesEnum.CAM_TRYING_TO_RUN_AFTER_HIBERNATION, camName);
                }
            }
        return null;
    }
}
