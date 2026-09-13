package com.icaroerasmo.messaging;

import com.icaroerasmo.enums.MessagesEnum;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class NotificationPublisherTest {

    @Test
    void publishTextSynchronousSendsMessageInCallingThread() {
        RecordingRabbitTemplate rabbit = new RecordingRabbitTemplate();
        NotificationPublisher publisher = new NotificationPublisher(rabbit);

        publisher.publishTextSynchronous(MessagesEnum.RECORDER_STOPPING);

        assertEquals(1, rabbit.sent.size());
        NotificationMessage message = (NotificationMessage) rabbit.sent.get(0);
        assertEquals(MessagesEnum.RECORDER_STOPPING.name(), message.template());
        assertEquals("recorder", message.sender());
        assertEquals(NotificationMessage.MediaType.TEXT, message.mediaType());
        // Recorded on the calling thread means the send happened synchronously.
        assertNotEquals("", message.messageId());
    }

    /** RabbitTemplate capturing the payload instead of sending to a broker. */
    private static class RecordingRabbitTemplate extends RabbitTemplate {

        private final List<Object> sent = new ArrayList<>();

        @Override
        public void convertAndSend(String exchange, String routingKey, Object message) throws AmqpException {
            sent.add(message);
        }
    }
}