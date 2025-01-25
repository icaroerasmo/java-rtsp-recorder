package com.icaroerasmo.enums;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@AllArgsConstructor
public enum MessagesEnum {
    RCLONE_NO_LOGS("No logs found."),
    RCLONE_SUCCESS("Synchroniation has finished in %s successfully."),
    RCLONE_ERROR("Synchronization has failed in %s");

    private String message;
}
