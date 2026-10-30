package com.hotelbooking.events.infrastructure;

import com.hotelbooking.events.application.EventConsumer;
import com.hotelbooking.events.application.EventDeliveryProcessor;
import com.hotelbooking.events.config.OutboxProperties;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.worker-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class OutboxWorker {
    private final EventDeliveryProcessor processor;
    private final List<EventConsumer> consumers;
    private final OutboxProperties properties;

    /** Deterministic drain for operational/manual tests; scheduled execution uses one independent job per consumer. */
    public void run() {
        consumers.forEach(this::run);
    }

    public void run(EventConsumer consumer) {
        for (int i = 0; i < properties.batchSize(); i++) {
            if (!processor.processNext(consumer)) {
                break;
            }
        }
    }
}
