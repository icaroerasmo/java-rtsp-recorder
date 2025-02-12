package com.icaroerasmo.util;

import com.icaroerasmo.enums.MessagesEnum;
import com.icaroerasmo.properties.TelegramProperties;
import com.icaroerasmo.runners.TranslateShellRunner;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class TelegramUtil {

    private final TranslateShellRunner translateShellRunner;
    private final TelegramProperties telegramProperties;
    private final TelegramBot telegram;

    public void sendMessage(MessagesEnum message, String camName) {
        try {
            final SendMessage request = new SendMessage(telegramProperties.getChatId(),
                    translateShellRunner.translateText(
                            message.getMessage().formatted(camName)));
            telegram.execute(request);
        } catch (Exception e) {
            log.error("Error sending message to Telegram: {}", e.getMessage());
            log.debug("Error sending message to Telegram: {}", e.getMessage(), e);
        }
    }
}
