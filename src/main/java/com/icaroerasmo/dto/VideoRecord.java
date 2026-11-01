package com.icaroerasmo.dto;

import lombok.Builder;
import lombok.Data;

/**
 * One recorded video segment, as exposed by the recorder's {@code /api/records}
 * endpoint. Either local (still on disk) or remote (on Google Drive), identified
 * by filename.
 */
@Data
@Builder
public class VideoRecord {

    /** Camera name, parsed from the filename (e.g. {@code garagem1}). */
    private String camera;

    /** File name, e.g. {@code garagem12026-11-01_19-55-37.mkv}. */
    private String filename;

    /** Recording time as {@code yyyy-MM-dd HH:mm:ss}. */
    private String timestamp;

    /** Path relative to the rclone remote root (e.g. {@code 2026/novembro/...mkv}). */
    private String drivePath;

    /** Absolute local path when the file is still on disk, otherwise {@code null}. */
    private String localPath;

    private long size;

    /** True when the file is available locally (no Drive download needed). */
    private boolean local;
}
