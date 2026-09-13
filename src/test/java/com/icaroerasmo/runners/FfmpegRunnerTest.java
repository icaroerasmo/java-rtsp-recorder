package com.icaroerasmo.runners;

import com.icaroerasmo.enums.MessagesEnum;
import com.icaroerasmo.parsers.FfmpegCommandParser;
import com.icaroerasmo.properties.RtspProperties;
import com.icaroerasmo.storage.FutureStorage;
import com.icaroerasmo.testutils.FakeFfmpeg;
import com.icaroerasmo.testutils.RecordingPublisher;
import com.icaroerasmo.util.Utilities;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class FfmpegRunnerTest {

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    private static final String CAM = "cam1";
    private static final String URL = "rtsp://user:pass@192.168.0.10:8554/live";

    @TempDir
    Path tempDir;

    Path tmpFolder;
    String fakeBin;
    FutureStorage futureStorage;
    RecordingPublisher publisher;
    FfmpegRunner runner;

    @AfterAll
    static void shutdownExecutor() {
        EXECUTOR.shutdownNow();
    }

    @BeforeEach
    void setUp() throws Exception {
        tmpFolder = Files.createDirectories(tempDir.resolve("tmpFolder"));
        fakeBin = FakeFfmpeg.install(tempDir).toString();
        futureStorage = new FutureStorage();
        publisher = new RecordingPublisher();
        runner = new FfmpegRunner(EXECUTOR, futureStorage, publisher, new Utilities());
    }

    @Test
    void okExitWritesSegmentsAndPublishesStarted() throws Exception {
        FakeFfmpeg.writeCtl(tmpFolder, CAM, "segments", "3");

        runner.run(CAM, command("ok-exit", 3, "500mm"));

        assertEquals(3, segmentList().size());
        assertEquals(3, countMkv(tmpFolder));
        assertTrue(publisher.contained(CAM, MessagesEnum.CAM_STARTED));
        assertEquals(0, publisher.attemptsFailed(CAM));
    }

    @Test
    void recoversAfterTransientFailures() throws Exception {
        FakeFfmpeg.setMode(tmpFolder, CAM, "fail-then-ok");
        FakeFfmpeg.writeCtl(tmpFolder, CAM, "failures", "2");

        runner.run(CAM, command("fail-then-ok", 5, "500mm"));

        assertEquals(2, publisher.attemptsFailed(CAM));
        assertTrue(publisher.contained(CAM, MessagesEnum.CAM_STARTED));
        assertEquals(2, segmentList().size());
    }

    @Test
    void crashDoesNotKillTheRunnerAndRecovers() throws Exception {
        FakeFfmpeg.setMode(tmpFolder, CAM, "crash");
        Path segmentsFile = tmpFolder.resolve("." + CAM + "_done_segments");

        Future<Void> future = EXECUTOR.submit(() -> runner.run(CAM, command("crash", 3, "500mm")));

        // Wait for the first (crashing) process to have written segments, then let the
        // next attempts succeed. Even if the flip races an attempt, ok-exit guarantees
        // a later attempt succeeds within maxRetries.
        awaitTrue(() -> entriesOf(segmentsFile) >= 1, TimeUnit.SECONDS.toMillis(10));
        FakeFfmpeg.setMode(tmpFolder, CAM, "ok-exit");

        future.get(10, TimeUnit.SECONDS);

        assertTrue(publisher.attemptsFailed(CAM) >= 1);
        assertTrue(publisher.contained(CAM, MessagesEnum.CAM_STARTED));
        assertTrue(entriesOf(segmentsFile) >= 1);
    }

    @Test
    void hibernatesThenKeepsRetryingAfterMaxAttempts() throws Exception {
        FakeFfmpeg.setMode(tmpFolder, CAM, "exit-fail");

        Future<Void> future = EXECUTOR.submit(() -> runner.run(CAM, command("exit-fail", 2, "500mm")));

        awaitTrue(() -> publisher.contained(MessagesEnum.CAM_MAX_ATTEMPTS_REACHED), TimeUnit.SECONDS.toMillis(10));
        awaitTrue(() -> publisher.contained(MessagesEnum.CAM_TRYING_TO_RUN_AFTER_HIBERNATION), TimeUnit.SECONDS.toMillis(10));
        assertTrue(publisher.attemptsFailed(CAM) >= 2);
        assertTrue(publisher.contained(CAM, MessagesEnum.CAM_ATTEMPT_FAILED));

        future.cancel(true);
    }

    private FfmpegCommandParser.FfmpegCommandParserBuilder command(
            String mode, int maxRetries, String retryWait) throws IOException {
        FakeFfmpeg.setMode(tmpFolder, CAM, mode);
        return FfmpegCommandParser.builder()
                .url(URL)
                .tmpPath(tmpFolder.toString())
                .cameraName(CAM)
                .transportProtocol(RtspProperties.TransportProtocol.TCP)
                .binaryPath(fakeBin)
                .hardwareAcceleration(RtspProperties.HardwareAcceleration.COPY)
                .doneSegmentsListSize(20)
                .videoDuration("5m")
                .timeout("5s")
                .maxRetries(maxRetries)
                .retryWait(retryWait);
    }

    private List<String> segmentList() throws IOException {
        Path segmentsFile = tmpFolder.resolve("." + CAM + "_done_segments");
        return Files.exists(segmentsFile)
                ? Files.readAllLines(segmentsFile).stream().filter(l -> !l.isBlank()).toList()
                : List.of();
    }

    private long entriesOf(Path file) {
        try {
            if (!Files.exists(file)) {
                return 0;
            }
            return Files.readAllLines(file).stream().filter(l -> !l.isBlank()).count();
        } catch (IOException e) {
            return 0;
        }
    }

    private long countMkv(Path folder) throws IOException {
        try (var stream = Files.list(folder)) {
            return stream.filter(p -> p.getFileName().toString().endsWith(".mkv")).count();
        }
    }

    private void awaitTrue(Supplier<Boolean> condition, long timeoutMs) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (Boolean.TRUE.equals(condition.get())) {
                return;
            }
            Thread.sleep(50);
        }
        fail("Condition not met within " + timeoutMs + "ms");
    }
}