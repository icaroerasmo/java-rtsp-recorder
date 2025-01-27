package com.icaroerasmo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MessagesEnum {
    RCLONE_NO_LOGS("No logs found."),
    RCLONE_SYNC_SUCCESS("Synchroniation has finished in %s successfully."),
    RCLONE_SYNC_ERROR("Synchronization has failed in %s"),
    RCLONE_DELETE_SUCCESS("Deletion has finished in %s successfully."),
    RCLONE_DELETE_ERROR("Deletion has failed in %s"),
    RCLONE_RMDIRS_SUCCESS("Deletion of empty folders has finished in %s successfully."),
    RCLONE_RMDIRS_ERROR("Deletion of empty folders has failed in %s"),
    RCLONE_DEDUPE_SUCCESS("Deletion of empty folders has finished in %s successfully."),
    RCLONE_DEDUPE_ERROR("Deletion of empty folders has failed in %s"),
    RCLONE_DELETE_START("Deletion started in %s."),
    RCLONE_RMDIRS_START("Deletion of empty folders started in %s."),
    RCLONE_DEDUPE_START("Deduplication started in %s."),
    RCLONE_SYNC_START("Synchronization started in %s."),;

    private String message;
}
