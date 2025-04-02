package com.icaroerasmo.config;

import com.icaroerasmo.properties.GeneralProperties;
import com.icaroerasmo.properties.RtspProperties;
import com.icaroerasmo.properties.TelegramProperties;
import com.pengrad.telegrambot.TelegramBot;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@RequiredArgsConstructor
public class BeansAndConfig {

    private final GeneralProperties generalProperties;
    private final TelegramProperties telegramProperties;

    @PostConstruct
    public void init() {
        setLocale();
        setTimezone();
    }

    @Bean
    public ExecutorService executorService(RtspProperties rtspProperties) {
        return Executors.newCachedThreadPool();
    }

    @Bean
    public TelegramBot bot() {
        return new TelegramBot(telegramProperties.getBotToken());
    }

    private void setLocale() {
        if(StringUtils.hasText(generalProperties.getLocale())) {
            Locale.setDefault(Locale.forLanguageTag(generalProperties.getLocale()));
        }
    }

    private void setTimezone() {
        if(StringUtils.hasText(generalProperties.getTimezone())) {
            TimeZone.setDefault(TimeZone.getTimeZone(generalProperties.getTimezone()));
        }
    }
}
