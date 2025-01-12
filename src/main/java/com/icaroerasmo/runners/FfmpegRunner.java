package com.icaroerasmo.runners;

import lombok.Getter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Getter
public class FfmpegRunner implements Supplier<Void> {

    private String command = "ffmpeg ";

    private FfmpegRunner(){};

    public FfmpegRunner(String... args) {
        command += String.join(" ", args);
    }

    @Override
    public Void get() {
        try {
            new ProcessBuilder(command).start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
