package com.icaroerasmo.config;

import com.icaroerasmo.dto.VideoRecord;
import com.icaroerasmo.services.RecordsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Lists recorded video segments (local + Google Drive) and streams a single
 * segment for playback. Local files are served directly (with HTTP Range support
 * for seeking); remote files are streamed on the fly from Google Drive via
 * {@code rclone cat}.
 */
@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
@Log4j2
public class RecordsController {

    private final RecordsService recordsService;

    @GetMapping
    public List<VideoRecord> list() {
        return recordsService.listAll();
    }

    @GetMapping("/{filename}/stream")
    public ResponseEntity<?> stream(@PathVariable String filename,
                                    @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) {
        Optional<Path> local = recordsService.resolveLocalPath(filename);
        if (local.isPresent()) {
            try {
                return serveLocal(local.get(), rangeHeader);
            } catch (IOException e) {
                log.warn("Failed to serve local record {}: {}", filename, e.getMessage());
                return ResponseEntity.status(502).build();
            }
        }

        String drivePath = recordsService.resolveDrivePath(filename);
        if (drivePath == null) {
            return ResponseEntity.notFound().build();
        }
        return serveRemote(filename, drivePath);
    }

    private ResponseEntity<?> serveLocal(Path path, String rangeHeader) throws IOException {
        Resource resource = new FileSystemResource(path);
        MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        if (rangeHeader != null && !rangeHeader.isBlank()) {
            List<HttpRange> ranges = HttpRange.parseRanges(rangeHeader);
            if (!ranges.isEmpty()) {
                HttpRange range = ranges.get(0);
                var region = range.toResourceRegion(resource);
                long start = region.getPosition();
                long end = start + region.getCount() - 1;
                long total = resource.contentLength();
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                        .contentType(mediaType)
                        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                        .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + total)
                        .body(region);
            }
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(resource.contentLength())
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(resource);
    }

    private ResponseEntity<byte[]> serveRemote(String filename, String drivePath) {
        try {
            byte[] bytes = recordsService.readDriveFile(drivePath);
            MediaType mediaType = MediaTypeFactory.getMediaType(filename)
                    .orElse(MediaType.APPLICATION_OCTET_STREAM);
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .contentLength(bytes.length)
                    .body(bytes);
        } catch (Exception e) {
            log.warn("Failed to stream Drive record {}: {}", filename, e.getMessage());
            return ResponseEntity.status(502).build();
        }
    }
}
