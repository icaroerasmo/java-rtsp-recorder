package com.icaroerasmo.util;

import com.icaroerasmo.properties.RtspProperties;
import org.springframework.stereotype.Component;

@Component
public class PropertiesUtil {
    public String cameraUrlParser(RtspProperties.Camera camera) {

        if(camera.getUrl() != null && !camera.getUrl().isBlank()) {
            return camera.getUrl();
        }

        return "rtsp://" + camera.getUsername()  + ":" + camera.getPassword() + "@" + camera.getHost() + ":" + camera.getPort() + "/"+camera.getFormat();
    }
}
