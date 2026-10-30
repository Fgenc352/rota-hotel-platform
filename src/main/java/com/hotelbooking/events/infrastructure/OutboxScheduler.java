package com.hotelbooking.events.infrastructure;

import com.hotelbooking.events.application.EventConsumer;
import com.hotelbooking.events.config.OutboxProperties;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.worker-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class OutboxScheduler implements DisposableBean {
    private final OutboxWorker worker;
    private final List<EventConsumer> consumers;
    private final OutboxProperties properties;
    private final ThreadPoolTaskScheduler scheduler;
    private final List<ScheduledFuture<?>> jobs = new ArrayList<>();

    @EventListener(ApplicationReadyEvent.class)
    public synchronized void start() {
        if (!jobs.isEmpty()) {
            return;
        }
        var names = new HashSet<String>();
        for (var consumer : consumers) {
            if (!names.add(consumer.name())) {
                throw new IllegalStateException("Duplicate outbox consumer name");
            }
        }
        for (var consumer : consumers) {
            jobs.add(scheduler.scheduleWithFixedDelay(() -> worker.run(consumer), properties.pollInterval()));
            log.info("event=outbox.consumer_scheduled consumer={} intervalMs={}", consumer.name(), properties.pollInterval().toMillis());
        }
    }

    @Override
    public synchronized void destroy() {
        jobs.forEach(job -> job.cancel(false));
        jobs.clear();
    }
}
