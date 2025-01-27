package com.icaroerasmo.runners;

import com.icaroerasmo.enums.MessagesEnum;
import com.icaroerasmo.parsers.CommandParser;
import com.icaroerasmo.parsers.RcloneSyncCommandParser;
import com.icaroerasmo.storage.FutureStorage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Log4j2
@RequiredArgsConstructor
public abstract class ARcloneRunner implements IRcloneRunner {

    private final ExecutorService executorService;
    private final FutureStorage futureStorage;

    @SneakyThrows
    public void start(CommandParser.CommandParserBuilder command, MessagesEnum message) {
        Process process = null;
        Future<StringBuilder> outputLogsFuture = null;
        Future<StringBuilder> errorLogsFuture = null;
        try {

            process = new ProcessBuilder(command.buildAsList()).start();

            Process finalProcess = process;

            outputLogsFuture = executorService.submit(() -> {

                final StringBuilder outputLogs = new StringBuilder();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(finalProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info("Rclone: {}", line);
                        outputLogs.append(line).append("\n");
                    }
                } catch (IOException e) {
                    log.debug("Stream closed for rclone output logs thread.");
                }

                return outputLogs;
            });

            errorLogsFuture = executorService.submit(() -> {

                final StringBuilder errorLogs = new StringBuilder();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(finalProcess.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info("Rclone: {}", line);
                        errorLogs.append(line).append("\n");
                    }
                } catch (IOException e) {
                    log.debug("Stream closed for rclone error logs thread.");
                }

                return errorLogs;
            });

            futureStorage.put("rclone", "outputLogsFuture", outputLogsFuture);
            futureStorage.put("rclone", "errorLogsFuture", errorLogsFuture);

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new RuntimeException("Rclone: execution failed with exit code " + exitCode);
            }

        } catch (Exception e) {

            if(e instanceof InterruptedException) {
                log.warn("Rclone: Interrupted.");
                Thread.currentThread().interrupt();
            } else {
                log.error("Rclone: Unexpected error occurred.");
                log.error("Rclone: {}", e.getMessage());
            }

            message = MessagesEnum.RCLONE_ERROR;

        } finally {

            if (process != null) {
                process.destroy();
            }

            final StringBuilder outputLogs = errorLogsFuture != null ? errorLogsFuture.get() : null;

            sendEndNotification(outputLogs, message);
        }
    }
}
