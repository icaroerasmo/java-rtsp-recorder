package com.icaroerasmo.util;

import com.icaroerasmo.properties.RtspProperties;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PropertiesUtil {
    public String cameraUrlParser(RtspProperties.Camera camera) {

        if(camera.getUrl() != null && !camera.getUrl().isBlank()) {
            return camera.getUrl();
        }

        return "rtsp://" + camera.getUsername()  + ":" + camera.getPassword() + "@" + camera.getHost() + ":" + camera.getPort() + "/" + camera.getFormat();
    }

    public String durationParser(String duration, TimeUnit timeUnit) {
        Pattern pattern = Pattern.compile("(?:(\\d+)d)?(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?(?:(\\d+)mm)?");
        Matcher matcher = pattern.matcher(duration);
        long millisecondsBuff = 0;

        if (matcher.matches()) {
            String days = matcher.group(1);
            String hours = matcher.group(2);
            String minutes = matcher.group(3);
            String seconds = matcher.group(4);
            String milliseconds = matcher.group(5);

            if (days != null) {
                millisecondsBuff += Long.parseLong(days) * 86400000;
            }
            if (hours != null) {
                millisecondsBuff += Long.parseLong(hours) * 3600000;
            }
            if (minutes != null) {
                millisecondsBuff += Long.parseLong(minutes) * 60000;
            }
            if (seconds != null) {
                millisecondsBuff += Long.parseLong(seconds) * 1000;
            }
            if (milliseconds != null) {
                millisecondsBuff += Long.parseLong(milliseconds);
            }
        }

        return String.valueOf(convertToUnit(timeUnit, millisecondsBuff));
    }

    public long convertToUnit(TimeUnit timeUnit, long millisecondsBuff) {
        return timeUnit.convert(millisecondsBuff, TimeUnit.MILLISECONDS);
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

    public long sizeOfFile(Path path) {
        return sizeOfFile(path.toFile());
    }

    public long sizeOfFile(File file) {

        if(file.isFile()) {
            return file.length();
        }

        long sizeOfFiles = 0;

        for(File newFile : Objects.requireNonNull(file.listFiles())) {
            sizeOfFiles += sizeOfFile(newFile);
        }

        sizeOfFiles += file.length();

        return sizeOfFiles;
    }
}
