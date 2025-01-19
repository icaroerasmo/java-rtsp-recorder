package com.icaroerasmo.config;

import com.icaroerasmo.properties.RtspProperties;
import com.icaroerasmo.properties.TelegramProperties;
import com.pengrad.telegrambot.TelegramBot;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@RequiredArgsConstructor
public class Beans {

    private final TelegramProperties telegramProperties;

    @Bean
    public ExecutorService executorService(RtspProperties rtspProperties) {
        return Executors.newFixedThreadPool((rtspProperties.getCameras().size()*3)+1);
    }

    @Bean
    public TelegramBot bot() {
        return new TelegramBot(telegramProperties.getBotToken());
    }
}
