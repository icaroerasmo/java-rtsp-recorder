package com.icaroerasmo.runners;

import lombok.Getter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Getter
public class FfmpegRunner implements Supplier<Void> {

    private String command = "ffmpeg ";
    private Consumer<String> outputLogsConsumer;
    private Consumer<String> errorLogsConsumer;

    private FfmpegRunner(){};

    public FfmpegRunner(String command, Consumer<String> outputLogsConsumer, Consumer<String> errorLogsConsumer) {
        this.command += command;
        this.outputLogsConsumer = outputLogsConsumer;
        this.errorLogsConsumer = errorLogsConsumer;
    }

    @Override
    public Void get() {
        runCommand();
        return null;
    }

    public void runCommand() {
        final int maxRetries = 3;
        int attempt = 0;
        boolean success = false;

        while (attempt < maxRetries && !success) {
            Process process = null;
            try {
                process = new ProcessBuilder(command.split(" ")).start();

                Process finalProcess = process;

                Thread outputThread = new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(finalProcess.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            outputLogsConsumer.accept(line);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Error reading ffmpeg output", e);
                    }
                });

                Thread errorThread = new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(finalProcess.getErrorStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            errorLogsConsumer.accept(line);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Error reading ffmpeg error output", e);
                    }
                });

                outputThread.start();
                errorThread.start();

                int exitCode = process.waitFor();
                outputThread.join();
                errorThread.join();

                if (exitCode == 0) {
                    success = true;
                } else {
                    throw new RuntimeException("ffmpeg execution failed with exit code " + exitCode);
                }
            } catch (IOException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                errorLogsConsumer.accept("Attempt " + (attempt + 1) + " failed: " + e.getMessage());
            } finally {
                if (process != null) {
                    process.destroy();
                }
            }
            attempt++;
        }

        if (!success) {
            throw new RuntimeException("ffmpeg execution failed after " + maxRetries + " attempts");
        }
    }
}
