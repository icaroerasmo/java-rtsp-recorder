package com.icaroerasmo.runners;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Locale;

@Log4j2
@Service
public class TranslateShellRunner {

    public String translateText(String text) {
        try {
            Locale locale = Locale.getDefault();
            Process process = new ProcessBuilder("trans", "-b", ":" + locale.toLanguageTag().toLowerCase(), text).start();
            String translatedText = "";
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                translatedText = reader.readLine();
            }
            return translatedText;
        } catch (Exception e) {
            log.error("Error translating text: {}", text, e);
            return text;
        }
    }
}