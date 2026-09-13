package com.icaroerasmo.jobs;

import com.icaroerasmo.enums.MessagesEnum;
import com.icaroerasmo.messaging.NotificationPublisher;
import com.icaroerasmo.properties.JavaRtspProperties;
import com.icaroerasmo.properties.RcloneProperties;
import com.icaroerasmo.properties.RtspProperties;
import com.icaroerasmo.properties.StorageProperties;
import com.icaroerasmo.services.FfmpegService;
import com.icaroerasmo.storage.FutureStorage;
import com.icaroerasmo.util.PropertiesUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CamCheckerScheduledTaskTest {

    @Test
    void exceptionOnOneCameraDoesNotPreventCheckingOthers(@TempDir Path tempDir) throws IOException {
        RtspProperties rtspProperties = new RtspProperties();
        rtspProperties.setCameras(List.of(camera("a"), camera("b")));

        StorageProperties storageProperties = new StorageProperties();
        storageProperties.setTmpFolder(tempDir.toString());

        JavaRtspProperties props =
                new JavaRtspProperties(rtspProperties, storageProperties, new RcloneProperties());
        FutureStorage futureStorage = new FailingFutureStorage("a");
        FfmpegServiceRecorder recorder = new FfmpegServiceRecorder();
        CamCheckerScheduledTask task = new CamCheckerScheduledTask(
                new NotificationPublisherStub(), props, futureStorage, recorder, new PropertiesUtil());

        task.checkIfCamsAreOnline();

        // Camera b must still be evaluated even though camera a threw during its check.
        assertTrue(recorder.started.contains("b"));
        assertFalse(recorder.started.contains("a"));
    }

    @Test
    void runningCameraWithFreshSegmentListIsNotRestarted(@TempDir Path tempDir) throws IOException {
        RtspProperties rtspProperties = new RtspProperties();
        rtspProperties.setCameras(List.of(camera("b")));

        StorageProperties storageProperties = new StorageProperties();
        storageProperties.setTmpFolder(tempDir.toString());

        JavaRtspProperties props =
                new JavaRtspProperties(rtspProperties, storageProperties, new RcloneProperties());
        FutureStorage futureStorage = new FutureStorage();
        futureStorage.put("b", "main", new RunningFuture());
        Files.write(tempDir.resolve(".b_done_segments"), new byte[]{1});

        FfmpegServiceRecorder recorder = new FfmpegServiceRecorder();
        CamCheckerScheduledTask task = new CamCheckerScheduledTask(
                new NotificationPublisherStub(), props, futureStorage, recorder, new PropertiesUtil());

        task.checkIfCamsAreOnline();

        assertTrue(recorder.started.isEmpty());
    }

    @Test
    void notRunningCameraIsRestarted(@TempDir Path tempDir) throws IOException {
        RtspProperties rtspProperties = new RtspProperties();
        rtspProperties.setCameras(List.of(camera("c")));

        StorageProperties storageProperties = new StorageProperties();
        storageProperties.setTmpFolder(tempDir.toString());

        JavaRtspProperties props =
                new JavaRtspProperties(rtspProperties, storageProperties, new RcloneProperties());
        FfmpegServiceRecorder recorder = new FfmpegServiceRecorder();
        CamCheckerScheduledTask task = new CamCheckerScheduledTask(
                new NotificationPublisherStub(), props, new FutureStorage(), recorder, new PropertiesUtil());

        task.checkIfCamsAreOnline();

        assertTrue(recorder.started.contains("c"));
    }

    private static RtspProperties.Camera camera(String name) {
        RtspProperties.Camera camera = new RtspProperties.Camera();
        camera.setName(name);
        return camera;
    }

    /** Future that reports RUNNING state without launching any thread. */
    private static class RunningFuture implements Future<Void> {
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return false;
        }

        @Override
        public Void get() {
            return null;
        }

        @Override
        public Void get(long timeout, TimeUnit unit) {
            return null;
        }

        @Override
        public State state() {
            return State.RUNNING;
        }
    }

    /** FutureStorage whose isRunning throws for one offending camera. */
    private static class FailingFutureStorage extends FutureStorage {
        private final String failingCam;

        private FailingFutureStorage(String failingCam) {
            this.failingCam = failingCam;
        }

        @Override
        public boolean isRunning(String name) {
            if (name.equals(failingCam)) {
                throw new RuntimeException("boom");
            }
            return super.isRunning(name);
        }
    }

    /** No-op publisher so tests never hit RabbitMQ. */
    private static class NotificationPublisherStub extends NotificationPublisher {
        private NotificationPublisherStub() {
            super(null);
        }

        @Override
        public void publishText(MessagesEnum template, Object... args) {
        }
    }

    /** FfmpegService that records start() calls instead of spawning ffmpeg. */
    private static class FfmpegServiceRecorder extends FfmpegService {
        private final List<String> started = new ArrayList<>();

        private FfmpegServiceRecorder() {
            super(null, null, null, null, null, null, null);
        }

        @Override
        public void start(String camName) {
            started.add(camName);
        }

        @Override
        public void stop(String camName) {
        }
    }
}