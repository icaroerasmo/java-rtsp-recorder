package com.icaroerasmo.testutils;

import com.icaroerasmo.enums.MessagesEnum;
import com.icaroerasmo.messaging.NotificationPublisher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Non-@Async publisher that records every published notification in memory, so tests
 * can assert on the exact messages (and arguments) the app produced.
 */
public class RecordingPublisher extends NotificationPublisher {

    public record Message(MessagesEnum template, Object[] args) {

        @Override
        public String toString() {
            return template.name() + " " + Arrays.toString(args);
        }
    }

    private final List<Message> messages = Collections.synchronizedList(new ArrayList<>());

    public RecordingPublisher() {
        super(null);
    }

    @Override
    public void publishText(MessagesEnum template, Object... args) {
        messages.add(new Message(template, args));
    }

    @Override
    public void publishTextSynchronous(MessagesEnum template, Object... args) {
        messages.add(new Message(template, args));
    }

    @Override
    public void publishNoLogs(MessagesEnum template, Object... args) {
        messages.add(new Message(template, args));
    }

    public List<Message> messages() {
        synchronized (messages) {
            return List.copyOf(messages);
        }
    }

    public boolean contained(MessagesEnum template) {
        return messages().stream().anyMatch(m -> m.template() == template);
    }

    public boolean contained(String camName, MessagesEnum template) {
        return messages().stream()
                .anyMatch(m -> m.template() == template
                        && m.args().length > 0 && camName.equals(String.valueOf(m.args()[0])));
    }

    public long attemptsFailed(String camName) {
        return messages().stream()
                .filter(m -> m.template() == MessagesEnum.CAM_ATTEMPT_FAILED)
                .filter(m -> m.args().length > 0 && camName.equals(String.valueOf(m.args()[0])))
                .count();
    }
}