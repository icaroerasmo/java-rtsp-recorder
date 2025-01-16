package com.icaroerasmo.util;

import com.icaroerasmo.properties.RtspProperties;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PropertiesUtil {
    public String cameraUrlParser(RtspProperties.Camera camera) {

        if(camera.getUrl() != null && !camera.getUrl().isBlank()) {
            return camera.getUrl();
        }

        return "rtsp://" + camera.getUsername()  + ":" + camera.getPassword() + "@" + camera.getHost() + ":" + camera.getPort() + "/"+camera.getFormat();
    }
    public String durationParser(String duration, TimeUnit timeUnit) {
        Pattern pattern = Pattern.compile("(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?");
        Matcher matcher = pattern.matcher(duration);
        long milliseconds = 0;

        if (matcher.matches()) {
            String hours = matcher.group(1);
            String minutes = matcher.group(2);
            String seconds = matcher.group(3);

            if (hours != null) {
                milliseconds += Long.parseLong(hours) * 3600000;
            }
            if (minutes != null) {
                milliseconds += Long.parseLong(minutes) * 60000;
            }
            if (seconds != null) {
                milliseconds += Long.parseLong(seconds) * 1000;
            }
        }

        return String.valueOf(timeUnit.convert(milliseconds, TimeUnit.MILLISECONDS));
    }

    public long storageUnitConverter(String val, String targetUnit) {

        Pattern pattern = Pattern.compile("^(\\d+)(b|Kb|B|KB|MB|GB)$");
        Matcher matcher = pattern.matcher(val);

        String unit = null;
        Long value = null;

        if (matcher.matches()) {
            value = Long.parseLong(matcher.group(1));
            unit = matcher.group(2);
        }

        long bytes;

        switch (unit) {
            case "b":
                bytes = value / 8;
                break;
            case "Kb":
                bytes = value * 1024 / 8;
                break;
            case "B":
                bytes = value;
                break;
            case "KB":
                bytes = value * 1024;
                break;
            case "MB":
                bytes = value * 1024 * 1024;
                break;
            case "GB":
                bytes = value * 1024 * 1024 * 1024;
                break;
            default:
                throw new IllegalArgumentException("Invalid unit: " + unit);
        }

        switch (targetUnit) {
            case "b":
                return bytes * 8;
            case "Kb":
                return bytes * 8 / 1024;
            case "B":
                return bytes;
            case "KB":
                return bytes / 1024;
            case "MB":
                return bytes / 1024 / 1024;
            case "GB":
                return bytes / 1024 / 1024 / 1024;
            default:
                throw new IllegalArgumentException("Invalid target unit: " + targetUnit);
        }
    }
}
