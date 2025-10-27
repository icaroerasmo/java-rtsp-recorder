package com.icaroerasmo.util;

import com.icaroerasmo.enums.MessagesEnum;
import com.icaroerasmo.properties.TelegramProperties;
import com.icaroerasmo.services.TranslationService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendDocument;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class TelegramUtil {

    private final TranslationService translationService;
    private final TelegramProperties telegramProperties;
    private final TelegramBot telegram;

    public void sendMessage(MessagesEnum message, Object... params) {
        try {
            final SendMessage request = new SendMessage(telegramProperties.getChatId(),
                    translationService.getMessage(message, params));
            telegram.execute(request);
        } catch (Exception e) {
            log.error("Error sending message to Telegram: {}", e.getMessage());
            log.debug("Error sending message to Telegram: {}", e.getMessage(), e);
        }
    }

    public void sendRawMessage(String text) {
        try {
            final SendMessage request = new SendMessage(telegramProperties.getChatId(), text);
            telegram.execute(request);
        } catch (Exception e) {
            log.error("Error sending raw message to Telegram: {}", e.getMessage());
            log.debug("Error sending raw message to Telegram: {}", e.getMessage(), e);
        }
    }

    public void sendDocument(String filename, byte[] data, String caption) {
        try {
            SendDocument req = new SendDocument(telegramProperties.getChatId(), data).fileName(filename);
            if (caption != null) req.caption(caption);
            telegram.execute(req);
        } catch (Exception e) {
            log.error("Error sending document to Telegram: {}", e.getMessage());
            log.debug("Error sending document to Telegram: {}", e.getMessage(), e);
        }
    }

    // New helper to expose translated text through TelegramUtil
    public String getTranslation(MessagesEnum message, Object... params) {
        try {
            return translationService.getMessage(message, params);
        } catch (Exception e) {
            log.warn("Translation failed for {}: {}", message, e.getMessage());
            return message.getMessage();
        }
    }

    // Convenience: allow raw code translations
    public String getTranslation(String code, Object... params) {
        try {
            return translationService.getMessage(code, params);
        } catch (Exception e) {
            log.warn("Translation failed for code {}: {}", code, e.getMessage());
            return String.format(code, params);
        }
    }
}
