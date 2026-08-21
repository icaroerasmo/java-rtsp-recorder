package com.icaroerasmo.config;

import com.icaroerasmo.properties.GeneralProperties;
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

    @PostConstruct
    public void init() {
        setLocale();
        setTimezone();
    }

    @Bean
    public ExecutorService executorService() {
        return Executors.newCachedThreadPool();
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
