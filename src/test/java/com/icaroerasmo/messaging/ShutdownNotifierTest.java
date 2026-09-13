package com.icaroerasmo.messaging;

import com.icaroerasmo.enums.MessagesEnum;
import com.icaroerasmo.testutils.RecordingPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.support.GenericApplicationContext;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ShutdownNotifierTest {

    @Test
    void publishesRecorderStoppingOnContextClosed() {
        RecordingPublisher publisher = new RecordingPublisher();
        ShutdownNotifier notifier = new ShutdownNotifier(publisher);

        notifier.onApplicationEvent(new ContextClosedEvent(new GenericApplicationContext()));

        assertTrue(publisher.contained(MessagesEnum.RECORDER_STOPPING));
    }
}