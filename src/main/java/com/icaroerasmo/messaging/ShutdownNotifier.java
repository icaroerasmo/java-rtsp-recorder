package com.icaroerasmo.messaging;

import com.icaroerasmo.enums.MessagesEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

/**
 * Sends a synchronous "app is stopping" notification when the Spring context closes
 * (e.g. on SIGTERM/SIGINT via the JVM shutdown hook). The publish must be synchronous:
 * during shutdown the @Async task executor is already being torn down, so messages
 * submitted through it are silently dropped.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class ShutdownNotifier implements ApplicationListener<ContextClosedEvent> {

    private final NotificationPublisher publisher;

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        log.info("Application shutting down, sending RECORDER_STOPPING notification");
        publisher.publishTextSynchronous(MessagesEnum.RECORDER_STOPPING);
    }
}