package com.icaroerasmo.config;

import com.icaroerasmo.properties.GeneralProperties;
import com.icaroerasmo.properties.RtspProperties;
import com.icaroerasmo.properties.TelegramProperties;
import com.pengrad.telegrambot.TelegramBot;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@RequiredArgsConstructor
public class BeansAndConfig {

    private final GeneralProperties generalProperties;
    private final TelegramProperties telegramProperties;

    @PostConstruct
    public void init() {
        Locale.setDefault(Locale.forLanguageTag(generalProperties.getLocale()));
    }

    @Bean
    public ExecutorService executorService(RtspProperties rtspProperties) {
        return Executors.newFixedThreadPool((rtspProperties.getCameras().size()*3)+3);
    }

    @Bean
    public TelegramBot bot() {
        return new TelegramBot(telegramProperties.getBotToken());
    }
}
