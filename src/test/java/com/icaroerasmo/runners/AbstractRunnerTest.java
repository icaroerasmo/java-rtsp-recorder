package com.icaroerasmo.runners;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractRunnerTest extends AbstractRunner {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    AbstractRunnerTest() {
        super(EXECUTOR);
    }

    @AfterAll
    static void shutdownExecutor() {
        EXECUTOR.shutdownNow();
    }

    @Test
    void launchLogListenerReadsAllLines() throws Exception {
        InputStream input = new ByteArrayInputStream("line1\nline2\n".getBytes());

        Future<StringBuilder> future = launchLogListener(input, "TestRunner", "error description");

        assertEquals("line1\nline2\n", future.get().toString());
    }

    @Test
    void launchLogListenerReturnsEmptyWhenStreamEmpty() throws Exception {
        InputStream input = new ByteArrayInputStream(new byte[0]);

        Future<StringBuilder> future = launchLogListener(input, "TestRunner", "error description");

        assertEquals("", future.get().toString());
    }

    @Test
    void launchLogListenerWithThrowErrorPropagatesRuntimeException() {
        InputStream failing = failingInputStream();
        Future<StringBuilder> future = launchLogListener(failing, "TestRunner", "error description", true);

        ExecutionException exception = assertThrows(ExecutionException.class, future::get);
        assertTrue(exception.getCause() instanceof RuntimeException);
    }

    @Test
    void launchLogListenerWithoutThrowErrorSwallowsIOException() throws Exception {
        InputStream failing = failingInputStream();
        Future<StringBuilder> future = launchLogListener(failing, "TestRunner", "error description", false);

        assertEquals("", future.get().toString());
    }

    private static InputStream failingInputStream() {
        return new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("boom");
            }
        };
    }
}
