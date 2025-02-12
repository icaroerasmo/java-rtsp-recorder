package com.icaroerasmo.util;

import com.icaroerasmo.enums.MessagesEnum;
import com.icaroerasmo.properties.TelegramProperties;
import com.icaroerasmo.runners.TranslateShellRunner;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TelegramUtil {

    private final TranslateShellRunner translateShellRunner;
    private final TelegramProperties telegramProperties;
    private final TelegramBot telegram;

    public void sendMessage(MessagesEnum message, String camName) {
        final SendMessage request = new SendMessage(telegramProperties.getChatId(),
                translateShellRunner.translateText(
                        message.getMessage().formatted(camName)));
        telegram.execute(request);
    }
}
