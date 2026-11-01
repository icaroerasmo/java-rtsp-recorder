package com.icaroerasmo.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icaroerasmo.dto.VideoRecord;
import com.icaroerasmo.properties.RcloneProperties;
import com.icaroerasmo.properties.StorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Lists recorded video segments from the local records folder and from Google
 * Drive (via {@code rclone lsjson}). Local files win over remote ones when the
 * filename matches.
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class RecordsService {

    /** Trailing timestamp in the filename: {@code yyyy-MM-dd_HH-mm-ss}. */
    private static final Pattern TIMESTAMP_PATTERN =
            Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})_(\\d{2})-(\\d{2})-(\\d{2})\\.mkv$");

    private final RcloneProperties rcloneProperties;
    private final StorageProperties storageProperties;
    private final ObjectMapper objectMapper;

    public List<VideoRecord> listAll() {
        Map<String, VideoRecord> byFilename = new LinkedHashMap<>();
        for (VideoRecord local : listLocal()) {
            byFilename.put(local.getFilename(), local);
        }
        for (VideoRecord remote : listDrive()) {
            byFilename.putIfAbsent(remote.getFilename(), remote);
        }
        return byFilename.values().stream()
                .sorted(Comparator.comparing(VideoRecord::getTimestamp).reversed())
                .toList();
    }

    /**
     * Resolves the absolute local path for a segment filename, or empty when the
     * file is no longer on disk. The path is derived from the filename (the
     * recorder stores files under {@code records/ANO/mês/DIA/HORA/camera/}).
     */
    public Optional<Path> resolveLocalPath(String filename) {
        Parsed parsed = parse(filename);
        if (parsed == null) {
            return Optional.empty();
        }
        Path path = Paths.get(storageProperties.getRecordsFolder(),
                String.valueOf(parsed.year()), monthName(parsed.month()),
                String.valueOf(parsed.day()), String.valueOf(parsed.hour()),
                parsed.camera(), filename);
        return Files.exists(path) ? Optional.of(path) : Optional.empty();
    }

    /**
     * Resolves the path relative to the rclone remote root (e.g.
     * {@code 2026/novembro/1/19/garagem1/...mkv}). The Drive layout mirrors the
     * local records folder, so it can be derived from the filename alone.
     */
    public String resolveDrivePath(String filename) {
        Parsed parsed = parse(filename);
        if (parsed == null) {
            return null;
        }
        return String.join("/", String.valueOf(parsed.year()), monthName(parsed.month()),
                String.valueOf(parsed.day()), String.valueOf(parsed.hour()),
                parsed.camera(), filename);
    }

    /**
     * Downloads a remote segment from Google Drive into memory using
     * {@code rclone cat}. Buffered (rather than streamed) so the controller can
     * return it as a plain byte body — the recorder serves few concurrent streams,
     * and segments are ~40 MB max.
     */
    public byte[] readDriveFile(String drivePath) throws IOException, InterruptedException {
        String remote = rcloneProperties.getDestinationFolder() + "/" + drivePath;
        List<String> command = new ArrayList<>();
        command.add("rclone");
        command.add("cat");
        command.add(remote);
        command.add("--config");
        command.add(rcloneProperties.getConfigLocation());

        Process process = new ProcessBuilder(command).start();
        byte[] bytes;
        try (java.io.InputStream in = process.getInputStream()) {
            bytes = in.readAllBytes();
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String error = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            throw new IOException("rclone cat exited with code " + exitCode + ": " + error);
        }
        return bytes;
    }

    private List<VideoRecord> listLocal() {
        List<VideoRecord> result = new ArrayList<>();
        Path recordsFolder = Paths.get(storageProperties.getRecordsFolder());
        if (!Files.isDirectory(recordsFolder)) {
            log.warn("Local records folder not found: {}", recordsFolder);
            return result;
        }
        try (Stream<Path> stream = Files.walk(recordsFolder)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".mkv"))
                    .forEach(path -> {
                        String filename = path.getFileName().toString();
                        VideoRecord record = toRecord(filename, false);
                        if (record == null) {
                            return;
                        }
                        record.setLocalPath(path.toString());
                        record.setLocal(true);
                        try {
                            record.setSize(Files.size(path));
                        } catch (IOException ignored) {
                            // keep size 0 when unreadable
                        }
                        result.add(record);
                    });
        } catch (IOException e) {
            log.warn("Failed to list local records: {}", e.getMessage());
        }
        return result;
    }

    private List<VideoRecord> listDrive() {
        List<VideoRecord> result = new ArrayList<>();
        try {
            String json = runRcloneLsjson();
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray()) {
                return result;
            }
            for (JsonNode node : root) {
                if (node.path("IsDir").asBoolean(false)) {
                    continue;
                }
                String name = node.path("Name").asText();
                if (!name.endsWith(".mkv")) {
                    continue;
                }
                VideoRecord record = toRecord(name, false);
                if (record == null) {
                    continue;
                }
                record.setDrivePath(node.path("Path").asText());
                record.setSize(node.path("Size").asLong(0));
                record.setLocal(false);
                result.add(record);
            }
        } catch (Exception e) {
            // Fail loudly: a partial list (local only) would make the dashboard
            // reconciliation delete every Drive-only record from Elasticsearch.
            log.error("Failed to list Drive records: {}", e.getMessage());
            throw new IllegalStateException("Failed to list Drive records", e);
        }
        return result;
    }

    private VideoRecord toRecord(String filename, boolean local) {
        Parsed parsed = parse(filename);
        if (parsed == null) {
            return null;
        }
        return VideoRecord.builder()
                .camera(parsed.camera())
                .filename(filename)
                .timestamp(parsed.timestamp())
                .local(local)
                .build();
    }

    private static String monthName(int month) {
        return Month.of(month).getDisplayName(TextStyle.FULL, Locale.getDefault());
    }

    /** Parses camera, timestamp and path components out of a segment filename. */
    private Parsed parse(String filename) {
        Matcher matcher = TIMESTAMP_PATTERN.matcher(filename);
        if (!matcher.find()) {
            return null;
        }
        String camera = filename.substring(0, matcher.start());
        int year = Integer.parseInt(matcher.group(1));
        int month = Integer.parseInt(matcher.group(2));
        int day = Integer.parseInt(matcher.group(3));
        int hour = Integer.parseInt(matcher.group(4));
        String timestamp = String.format("%04d-%02d-%02d %02d:%s:%s",
                year, month, day, hour, matcher.group(5), matcher.group(6));
        return new Parsed(camera, filename, timestamp, year, month, day, hour);
    }

    private String runRcloneLsjson() throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("rclone");
        command.add("lsjson");
        command.add(rcloneProperties.getDestinationFolder());
        command.add("--recursive");
        command.add("--fast-list");
        command.add("--config");
        command.add(rcloneProperties.getConfigLocation());

        Process process = new ProcessBuilder(command).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String error = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            throw new IOException("rclone lsjson exited with code " + exitCode + ": " + error);
        }
        return output;
    }

    private record Parsed(String camera, String filename, String timestamp,
                          int year, int month, int day, int hour) {}
}
