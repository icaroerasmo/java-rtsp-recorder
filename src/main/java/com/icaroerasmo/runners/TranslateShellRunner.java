package com.icaroerasmo.runners;

import com.icaroerasmo.util.Utilities;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Locale;

@Log4j2
@Service
@RequiredArgsConstructor
public class TranslateShellRunner {

    private final Utilities utilities;

    public String translateText(String text) {
        Process process = null;
        try {
            Locale locale = Locale.getDefault();
            process = new ProcessBuilder(
                    "trans", "-e", "bing", "-b", ":" + locale.toLanguageTag().toLowerCase(), text).start();
            String translatedText = "";
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                translatedText = reader.readLine();
            }
            return translatedText;
        } catch (Exception e) {
            log.error("Error translating text: {}", text, e);
            return text;
        } finally {
            utilities.killProcess(process);
        }
    }
}
