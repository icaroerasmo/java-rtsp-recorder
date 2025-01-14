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
}
