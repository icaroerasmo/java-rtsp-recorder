package com.icaroerasmo.util;

import com.icaroerasmo.enums.MessagesEnum;

public final class TelegramMessageFormatter {

    private TelegramMessageFormatter() {}

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public static String format(MessagesEnum messageType, String translatedText) {
        String content = translatedText == null ? "" : translatedText;

        // Basic HTML formatting + emoji prefix depending on message type
        String prefix = "";
        switch (messageType) {
            case RCLONE_SYNC_SUCCESS:
            case RCLONE_DELETE_SUCCESS:
            case RCLONE_DEDUPE_SUCCESS:
            case RCLONE_RMDIRS_SUCCESS:
                prefix = "✅ ";
                break;
            case RCLONE_SYNC_ERROR:
            case RCLONE_DELETE_ERROR:
            case RCLONE_DEDUPE_ERROR:
            case RCLONE_RMDIRS_ERROR:
                prefix = "❌ ";
                break;
            case RCLONE_DELETE_START:
            case RCLONE_RMDIRS_START:
            case RCLONE_DEDUPE_START:
            case RCLONE_SYNC_START:
                prefix = "▶️ ";
                break;
            case CAM_INITIATING:
            case CAM_STARTED:
                prefix = "🎬 ";
                break;
            case CAM_STOPPED:
                prefix = "⏸️ ";
                break;
            case CAM_ATTEMPT_FAILED:
            case CAM_MAX_ATTEMPTS_REACHED:
            case CAM_TRYING_TO_RUN_AFTER_HIBERNATION:
                prefix = "⚠️ ";
                break;
            case CAM_CHECKER_NOT_RUNNING:
                prefix = "🛑 ";
                break;
            case CAM_CHECKER_RECOVERED:
                prefix = "🔁 ";
                break;
            default:
                prefix = "📣 ";
        }

        String escaped = escapeHtml(content);
        return prefix + escaped;
    }
}
