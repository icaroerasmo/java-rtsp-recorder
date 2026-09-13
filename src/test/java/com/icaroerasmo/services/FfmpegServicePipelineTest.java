package com.icaroerasmo.services;

import com.icaroerasmo.enums.MessagesEnum;
import com.icaroerasmo.jobs.CamCheckerScheduledTask;
import com.icaroerasmo.jobs.FileMoverScheduledTask;
import com.icaroerasmo.properties.JavaRtspProperties;
import com.icaroerasmo.properties.RcloneProperties;
import com.icaroerasmo.properties.RtspProperties;
import com.icaroerasmo.properties.StorageProperties;
import com.icaroerasmo.runners.FfmpegRunner;
import com.icaroerasmo.storage.FutureStorage;
import com.icaroerasmo.testutils.FakeFfmpeg;
import com.icaroerasmo.testutils.RecordingPublisher;
import com.icaroerasmo.util.FfmpegUtil;
import com.icaroerasmo.util.PropertiesUtil;
import com.icaroerasmo.util.Utilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Full pipeline integration test: real FfmpegService + FfmpegRunner + FileMover +
 * CamChecker wired together, real (fake) ffmpeg processes that write actual .mkv files,
 * forcing failures at each step of video creation and proving the app recovers and keeps
 * recording and moving segments.
 */
class FfmpegServicePipelineTest {

    @TempDir
    Path tempDir;

    Path tmpFolder;
    Path recordsFolder;
    ExecutorService executor;
    RecordingPublisher publisher;
    FutureStorage futureStorage;
    RtspProperties rtspProperties;
    FfmpegService service;
    FileMoverScheduledTask mover;
    CamCheckerScheduledTask checker;
    List<RtspProperties.Camera> cameras;

    @BeforeEach
    void setUp() throws Exception {
        tmpFolder = Files.createDirectories(tempDir.resolve("tmp"));
        recordsFolder = Files.createDirectories(tempDir.resolve("records"));

        String fakeBin = FakeFfmpeg.install(tempDir).toString();

        rtspProperties = new RtspProperties();
        rtspProperties.setBinaryPath(fakeBin);
        rtspProperties.setMaxRetries(3);
        rtspProperties.setRetryWait("500mm");
        rtspProperties.setVideoDuration("1s");

        StorageProperties storageProperties = new StorageProperties();
        storageProperties.setTmpFolder(tmpFolder.toString());
        storageProperties.setRecordsFolder(recordsFolder.toString());
        storageProperties.setMaxRecordsFolderSize("1GB");

        JavaRtspProperties javaRtspProperties =
                new JavaRtspProperties(rtspProperties, storageProperties, new RcloneProperties());

        executor = Executors.newCachedThreadPool();
        publisher = new RecordingPublisher();
        futureStorage = new FutureStorage();
        FfmpegUtil ffmpegUtil = new FfmpegUtil(storageProperties, new PropertiesUtil());
        PropertiesUtil propertiesUtil = new PropertiesUtil();
        FfmpegRunner runner = new FfmpegRunner(executor, futureStorage, publisher, new Utilities());
        service = new FfmpegService(
                javaRtspProperties, executor, runner, propertiesUtil,
                futureStorage, ffmpegUtil, publisher);
        mover = new FileMoverScheduledTask(ffmpegUtil, javaRtspProperties);
        checker = new CamCheckerScheduledTask(
                publisher, javaRtspProperties, futureStorage, service, propertiesUtil);
    }

    @AfterEach
    void tearDown() {
        if (cameras != null) {
            cameras.forEach(cam -> {
                try {
                    service.stop(cam.getName());
                } catch (Exception e) {
                    // best-effort cleanup
                }
            });
        }
        executor.shutdownNow();
    }

    @Test
    void healthyCameraKeepsRecordingWhileOfflineCameraRetries() throws Exception {
        cameras = List.of(camera("camA"), camera("camB"));
        rtspProperties.setCameras(cameras);
        FakeFfmpeg.setMode(tmpFolder, "camA", "record");
        FakeFfmpeg.writeCtl(tmpFolder, "camA", "max-segments", "8");
        FakeFfmpeg.setMode(tmpFolder, "camB", "exit-fail");

        service.init();

        // camA started recording; camB is offline and keeps failing without blocking camA.
        awaitTrue(() -> publisher.contained("camA", MessagesEnum.CAM_STARTED), 15000);
        awaitTrue(() -> publisher.attemptsFailed("camB") >= 1, 15000);

        // First batch of segments produced and moved to records.
        awaitTrue(() -> tryCall(() -> allSegmentsFreshAndExist("camA")), 15000);
        mover.filesMover();
        assertEquals(8, mkvCount(recordsFolder, "camA"));
        assertEquals(0, mkvCount(tmpFolder, "camA"));

        // Simulate ffmpeg crashing mid-recording: runner must detect the exit and retry.
        long oldPid = futureStorage.getProcess("camA").pid();
        futureStorage.getProcess("camA").destroyForcibly();
        awaitTrue(() -> futureStorage.getProcess("camA") != null
                        && futureStorage.getProcess("camA").isAlive()
                        && futureStorage.getProcess("camA").pid() != oldPid
                        && publisher.attemptsFailed("camA") >= 1, 15000);

        // New recording process produced fresh segments: moved a second time.
        awaitTrue(() -> tryCall(() -> allSegmentsFreshAndExist("camA")), 15000);
        mover.filesMover();
        assertEquals(16, mkvCount(recordsFolder, "camA"));
        assertEquals(0, mkvCount(tmpFolder, "camA"));
        assertTrue(publisher.attemptsFailed("camB") >= 1);
    }

    @Test
    void zombieCameraIsKilledAndRestartedByChecker() throws Exception {
        cameras = List.of(camera("camC"));
        rtspProperties.setCameras(cameras);
        FakeFfmpeg.setMode(tmpFolder, "camC", "hang");

        service.init();

        Process firstProcess = awaitAliveProcess("camC");
        long firstPid = firstProcess.pid();

        // Let the segment list go stale (videoDuration=1s -> stale threshold 3s).
        Path segmentsFile = tmpFolder.resolve(".camC_done_segments");
        awaitTrue(() -> tryCall(() -> Files.exists(segmentsFile)
                        && System.currentTimeMillis()
                        - Files.getLastModifiedTime(segmentsFile).toMillis() > 3500),
                8000);

        checker.checkIfCamsAreOnline();

        assertTrue(publisher.contained(MessagesEnum.CAM_CHECKER_NOT_RECORDING));
        assertTrue(publisher.contained("camC", MessagesEnum.CAM_INITIATING));
        Process restarted = awaitAliveProcess("camC");
        assertNotEquals(firstPid, restarted.pid());
    }

    private boolean allSegmentsFreshAndExist(String camName) throws IOException {
        Path segmentsFile = tmpFolder.resolve("." + camName + "_done_segments");
        if (!Files.exists(segmentsFile)) {
            return false;
        }
        List<String> entries = Files.readAllLines(segmentsFile).stream()
                .filter(l -> !l.isBlank()).toList();
        if (entries.size() < 8) {
            return false;
        }
        for (String entry : entries) {
            if (!Files.exists(tmpFolder.resolve(entry))) {
                return false;
            }
        }
        return true;
    }

    private Process awaitAliveProcess(String camName) throws Exception {
        for (int i = 0; i < 200; i++) {
            Process process = futureStorage.getProcess(camName);
            if (process != null && process.isAlive()) {
                return process;
            }
            Thread.sleep(50);
        }
        return fail("No alive process for " + camName + " within timeout");
    }

    private long mkvCount(Path root, String camPrefix) {
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(p -> p.getFileName().toString().endsWith(".mkv"))
                    .filter(p -> p.getFileName().toString().startsWith(camPrefix))
                    .count();
        } catch (IOException e) {
            return 0;
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

    private boolean tryCall(java.util.concurrent.Callable<Boolean> condition) {
        try {
            return Boolean.TRUE.equals(condition.call());
        } catch (Exception e) {
            // the condition is usually probing transient FS state
            return false;
        }
    }

    private static RtspProperties.Camera camera(String name) {
        RtspProperties.Camera camera = new RtspProperties.Camera();
        camera.setName(name);
        camera.setUrl("rtsp://user:pass@192.168.0.10:8554/live");
        return camera;
    }
}