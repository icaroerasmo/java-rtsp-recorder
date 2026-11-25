package com.icaroerasmo.runners;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Log4j2
@AllArgsConstructor
public abstract class AbstractRunner {

    private static final int MAX_LOG_CHARS = 256 * 1024;

    private final ExecutorService executorService;

    protected Future<StringBuilder> launchLogListener(InputStream inputStream, String runnerPrefix, String errorDescription) {
        return launchLogListener(inputStream, runnerPrefix, errorDescription, false);
    }

    protected Future<StringBuilder> launchLogListener(InputStream inputStream, String runnerPrefix, String errorDescription, boolean throwError) {
        return executorService.submit(() -> {

            StringBuilder outputLogs = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("{}: {}", runnerPrefix, line);
                    outputLogs = appendBounded(outputLogs, line);
                }
            } catch (IOException e) {
                if(throwError) {
                    throw new RuntimeException(runnerPrefix+": "+errorDescription, e);
                }
                log.debug(errorDescription, e);
            }

            return outputLogs;
        });
    }

    private static StringBuilder appendBounded(StringBuilder logs, String line) {

        String text = line.length() > MAX_LOG_CHARS
                ? line.substring(line.length() - MAX_LOG_CHARS)
                : line;

        logs.append(text).append('\n');

        if (logs.length() <= MAX_LOG_CHARS) {
            return logs;
        }

        // A fresh builder releases the oversized backing char[]; setLength()
        // would keep it allocated and the memory would still leak.
        return new StringBuilder(logs.substring(logs.length() - MAX_LOG_CHARS));
    }
}
