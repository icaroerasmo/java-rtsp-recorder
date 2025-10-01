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

    private final ExecutorService executorService;

    protected Future<StringBuilder> launchLogListener(InputStream inputStream, String runnerPrefix, String errorDescription) {
        return launchLogListener(inputStream, runnerPrefix, errorDescription, false);
    }

    protected Future<StringBuilder> launchLogListener(InputStream inputStream, String runnerPrefix, String errorDescription, boolean throwError) {
        return executorService.submit(() -> {

            final StringBuilder outputLogs = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("{}: {}", runnerPrefix, line);
                    outputLogs.append(line).append("\n");
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
}
