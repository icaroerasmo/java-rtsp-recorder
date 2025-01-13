package com.icaroerasmo.runners;

import com.icaroerasmo.properties.JavaRtspProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Log4j2
@Component
@RequiredArgsConstructor
public class FfmpegRunner {

    private final ExecutorService executorService;

    @SneakyThrows
    public Void run(String camName, String command) {

        log.info("Cam {}: Running command: {}", camName, command);

        final int maxRetries = 3;
        int attempt = 0;
        boolean success = false;

        for(;;) {
            while (attempt < maxRetries && !success) {
                Process process = null;
                Future<Void> outputLogsFuture = null;
                Future<Void> errorLogsFuture = null;
                try {
                    process = new ProcessBuilder(command.split(" ")).start();

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

                    Thread.sleep(1000); // Wait for threads to finish reading output before checking

                    if (process.isAlive()) {
                        attempt = 0;
                    }

                    int exitCode = process.waitFor();

                    if (exitCode == 0) {
                        success = true;
                    } else {
                        throw new RuntimeException("Cam " + camName + ": ffmpeg execution failed with exit code " + exitCode);
                    }
                } catch (Exception e) {
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    log.warn("Cam {}: Attempt {} failed: {}", camName, attempt + 1, e.getMessage());
                } finally {
                    if (process != null) {
                        process.destroy();
                    }
                }
                attempt++;
            }

            if (!success) {
                log.error("Cam {}: ffmpeg execution failed after " + maxRetries + " attempts. Retrying in 5 minutes...", camName);
                Thread.sleep(300000);
            }
        }
    }
}
