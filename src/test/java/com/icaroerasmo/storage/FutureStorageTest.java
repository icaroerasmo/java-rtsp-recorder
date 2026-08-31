package com.icaroerasmo.storage;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FutureStorageTest {

    /** Thread-free Future stub whose state is derived from done/cancelled flags. */
    private static class StubFuture implements Future<Void> {
        private final boolean done;
        private final boolean cancelled;

        private StubFuture(boolean done, boolean cancelled) {
            this.done = done;
            this.cancelled = cancelled;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public Void get() {
            return null;
        }

        @Override
        public Void get(long timeout, TimeUnit unit) {
            return null;
        }
    }

    @Test
    void putAndGetReturnsSameFuture() {
        FutureStorage storage = new FutureStorage();
        Future<?> future = CompletableFuture.completedFuture(null);

        storage.put("cam1", "main", future);

        assertSame(future, storage.get("cam1", "main"));
    }

    @Test
    void getByNameReturnsThreadMap() {
        FutureStorage storage = new FutureStorage();
        Future<?> future = CompletableFuture.completedFuture(null);

        storage.put("cam1", "outputLogsFuture", future);

        Map<String, Future<?>> threadMap = storage.get("cam1");
        assertNotNull(threadMap);
        assertSame(future, threadMap.get("outputLogsFuture"));
    }

    @Test
    void getReturnsNullForUnknownName() {
        FutureStorage storage = new FutureStorage();
        assertNull(storage.get("unknown"));
        assertNull(storage.get("unknown", "main"));
    }

    @Test
    void getReturnsNullForUnknownThread() {
        FutureStorage storage = new FutureStorage();
        storage.put("cam1", "main", CompletableFuture.completedFuture(null));
        assertNull(storage.get("cam1", "missingThread"));
    }

    @Test
    void isRunningTrueForNonDoneFuture() {
        FutureStorage storage = new FutureStorage();
        storage.put("cam1", "main", new StubFuture(false, false));

        assertTrue(storage.isRunning("cam1"));
        assertTrue(storage.isRunning("cam1", "main"));
    }

    @Test
    void isRunningFalseForDoneFuture() {
        FutureStorage storage = new FutureStorage();
        storage.put("cam1", "main", new StubFuture(true, false));

        assertFalse(storage.isRunning("cam1"));
    }

    @Test
    void isRunningFalseWhenNoFutureStored() {
        FutureStorage storage = new FutureStorage();
        assertFalse(storage.isRunning("cam1"));
        assertFalse(storage.isRunning("cam1", "main"));
    }

    @Test
    void isRunningDefaultsToMainThread() {
        FutureStorage storage = new FutureStorage();
        storage.put("cam1", "main", new StubFuture(false, false));
        assertTrue(storage.isRunning("cam1"));

        // A future stored under a different thread name is not picked up by the default
        storage = new FutureStorage();
        storage.put("cam1", "otherThread", new StubFuture(false, false));
        assertFalse(storage.isRunning("cam1"));
    }

    @Test
    void deleteRemovesAllThreadsAndProcesses() {
        FutureStorage storage = new FutureStorage();
        storage.put("cam1", "main", new StubFuture(false, false));
        storage.put("cam1", "outputLogsFuture", new StubFuture(false, false));
        storage.putProcess("cam1", new FakeProcess());

        storage.delete("cam1");

        assertNull(storage.get("cam1"));
        assertNull(storage.get("cam1", "main"));
        assertNull(storage.getProcess("cam1"));
    }

    @Test
    void deleteByThreadNameRemovesOnlyThatThread() {
        FutureStorage storage = new FutureStorage();
        Future<?> kept = new StubFuture(false, false);
        Future<?> removed = new StubFuture(false, false);
        storage.put("cam1", "main", kept);
        storage.put("cam1", "outputLogsFuture", removed);

        storage.delete("cam1", "outputLogsFuture");

        assertSame(kept, storage.get("cam1", "main"));
        assertNull(storage.get("cam1", "outputLogsFuture"));
        // Thread map and processes are kept when deleting a single thread
        assertNotNull(storage.get("cam1"));
    }

    @Test
    void deleteOnUnknownNameIsNoOp() {
        FutureStorage storage = new FutureStorage();
        storage.delete("unknown");
        storage.delete("unknown", "main");
    }

    @Test
    void putProcessAndGetProcess() {
        FutureStorage storage = new FutureStorage();
        FakeProcess process = new FakeProcess();

        storage.putProcess("cam1", process);

        assertSame(process, storage.getProcess("cam1"));
        assertNull(storage.getProcess("unknown"));
    }

    @Test
    void putAndGetStartTime() {
        FutureStorage storage = new FutureStorage();

        storage.putStartTime("cam1", 12345L);

        assertEquals(12345L, storage.getStartTime("cam1"));
        assertEquals(0L, storage.getStartTime("unknown"));
    }

    @Test
    void deleteRemovesStartTime() {
        FutureStorage storage = new FutureStorage();
        storage.put("cam1", "main", new StubFuture(false, false));
        storage.putStartTime("cam1", 12345L);

        storage.delete("cam1");

        assertEquals(0L, storage.getStartTime("cam1"));
    }

    @Test
    void storesMultipleThreadsUnderSameName() {
        FutureStorage storage = new FutureStorage();
        Future<?> a = new StubFuture(false, false);
        Future<?> b = new StubFuture(false, false);

        storage.put("cam1", "main", a);
        storage.put("cam1", "outputLogsFuture", b);

        assertSame(a, storage.get("cam1", "main"));
        assertSame(b, storage.get("cam1", "outputLogsFuture"));
        assertEquals(2, storage.get("cam1").size());
    }

    /** Minimal Process stand-in (abstract class) without launching anything. */
    private static class FakeProcess extends Process {
        @Override
        public java.io.OutputStream getOutputStream() {
            return java.io.OutputStream.nullOutputStream();
        }

        @Override
        public java.io.InputStream getInputStream() {
            return java.io.InputStream.nullInputStream();
        }

        @Override
        public java.io.InputStream getErrorStream() {
            return java.io.InputStream.nullInputStream();
        }

        @Override
        public int waitFor() {
            return 0;
        }

        @Override
        public int exitValue() {
            return 0;
        }

        @Override
        public void destroy() {
        }
    }
}
