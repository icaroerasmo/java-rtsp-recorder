package com.icaroerasmo.util;

import com.icaroerasmo.properties.RtspProperties;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
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

        if(!matcher.matches()) {
            throw new IllegalArgumentException("Input string does not match the expected pattern.");
        }

        final String days = matcher.group(1);
        final String hours = matcher.group(2);
        final String minutes = matcher.group(3);
        final String seconds = matcher.group(4);
        final String milliseconds = matcher.group(5);

        long millisecondsBuff = 0;

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

        return String.valueOf(convertToUnit(timeUnit, millisecondsBuff));
    }

    public long convertToUnit(TimeUnit timeUnit, long millisecondsBuff) {
        return timeUnit.convert(millisecondsBuff, TimeUnit.MILLISECONDS);
    }

    public long storageUnitConverter(String val, String targetUnit) {

        Pattern pattern = Pattern.compile("^(\\d+)(b|Kb|B|KB|MB|GB)$");
        Matcher matcher = pattern.matcher(val);

        if(!matcher.matches()) {
            throw new IllegalArgumentException("Input string does not match the expected pattern.");
        }

        final long value = Long.parseLong(matcher.group(1));
        final String unit = matcher.group(2);

        long bytes = switch (unit) {
            case "b" -> value / 8;
            case "Kb" -> value * 1024 / 8;
            case "B" -> value;
            case "KB" -> value * 1024;
            case "MB" -> value * 1024 * 1024;
            case "GB" -> value * 1024 * 1024 * 1024;
            default -> throw new IllegalArgumentException("Invalid unit: " + unit);
        };

        return switch (targetUnit) {
            case "b" -> bytes * 8;
            case "Kb" -> bytes * 8 / 1024;
            case "B" -> bytes;
            case "KB" -> bytes / 1024;
            case "MB" -> bytes / 1024 / 1024;
            case "GB" -> bytes / 1024 / 1024 / 1024;
            default -> throw new IllegalArgumentException("Invalid target unit: " + targetUnit);
        };
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
