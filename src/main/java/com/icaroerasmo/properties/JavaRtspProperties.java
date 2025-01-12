package com.icaroerasmo.properties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Getter
@Component
@RequiredArgsConstructor
public class JavaRtspProperties {
    private final RtspProperties rtspProperties;
    private final StorageProperties storageProperties;
    private final TelegramProperties telegramProperties;
}
