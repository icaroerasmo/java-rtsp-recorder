package com.icaroerasmo.messaging;

import com.icaroerasmo.enums.MessagesEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Log4j2
@Service
@RequiredArgsConstructor
public class NotificationPublisher {

    private static final String SENDER = "recorder";
    private static final String EXCHANGE = "telegram.exchange";
    private static final String ROUTING_KEY = "telegram.notifications";

    private final RabbitTemplate rabbitTemplate;

    public void publishText(MessagesEnum template, Object... args) {
        publish(template, NotificationMessage.MediaType.TEXT, null, null, false, args);
    }

    public void publishNoLogs(MessagesEnum template, Object... args) {
        publish(template, NotificationMessage.MediaType.TEXT, null, null, true, args);
    }

    public void publishDocument(MessagesEnum template, String filename, byte[] payload, Object... args) {
        publish(template, NotificationMessage.MediaType.DOCUMENT, filename, payload, false, args);
    }

    private void publish(MessagesEnum template, NotificationMessage.MediaType mediaType,
                         String filename, byte[] payload, boolean appendNoLogs, Object... args) {
        final List<String> stringifiedArgs = args == null ? List.of() :
                Arrays.stream(args).map(String::valueOf).toList();

        final NotificationMessage message = new NotificationMessage(
                UUID.randomUUID().toString(),
                SENDER,
                mediaType,
                template.name(),
                stringifiedArgs,
                null,
                filename,
                payload,
                appendNoLogs);

        try {
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, message);
        } catch (Exception e) {
            log.error("Error publishing notification to RabbitMQ: {}", e.getMessage());
            log.debug("Error publishing notification to RabbitMQ", e);
        }
    }
}
