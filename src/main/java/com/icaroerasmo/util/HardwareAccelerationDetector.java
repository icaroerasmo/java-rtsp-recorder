package com.icaroerasmo.util;

import com.icaroerasmo.properties.RtspProperties.HardwareAcceleration;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resolve a aceleração de hardware efetiva para o host onde o recorder está
 * executando. O modo {@code AUTO} detecta a GPU disponível (NVIDIA via
 * {@code /dev/nvidia0}, VA-API via o device configurado em {@code rtsp.vaapi-device})
 * e cai para CPU quando nenhuma é encontrada.
 */
public final class HardwareAccelerationDetector {

    private static final String NVIDIA_DEVICE = "/dev/nvidia0";

    private HardwareAccelerationDetector() {
    }

    public static HardwareAcceleration resolve(HardwareAcceleration configured, String vaapiDevice) {
        if (configured == null) {
            return HardwareAcceleration.NONE;
        }
        if (configured != HardwareAcceleration.AUTO) {
            return configured;
        }
        return detect(NVIDIA_DEVICE, vaapiDevice);
    }

    static HardwareAcceleration detect(String nvidiaDevice, String vaapiDevice) {
        if (nvidiaDevice != null && Files.exists(Path.of(nvidiaDevice))) {
            return HardwareAcceleration.NVIDIA;
        }
        if (vaapiDevice != null && Files.exists(Path.of(vaapiDevice))) {
            return HardwareAcceleration.RADEON;
        }
        return HardwareAcceleration.CPU;
    }
}
