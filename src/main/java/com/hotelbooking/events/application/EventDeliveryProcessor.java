package com.hotelbooking.events.application;

import com.hotelbooking.contracts.reservation.ReservationSnapshot;
import com.hotelbooking.events.application.model.EventDelivery;
import com.hotelbooking.events.application.port.EventDeliveries;
import com.hotelbooking.shared.json.Json;
import com.hotelbooking.shared.logging.FailureDiagnostics;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventDeliveryProcessor {
    private final EventDeliveries repository;
    private final TransactionTemplate tx;
    private final Json json;
    private final MeterRegistry metrics;

    public boolean processNext(EventConsumer consumer) {
        var selected = new AtomicReference<EventDelivery>();
        String previous = MDC.get("traceId");
        try {
            if (consumer.requiresLocalTransaction()) {
                // CRM projection and acknowledgement commit together.
                return Boolean.TRUE.equals(tx.execute(status -> {
                    var next = repository.lockNext(consumer.name());
                    if (next.isEmpty()) {
                        return false;
                    }
                    selected.set(next.get());
                    accept(consumer, next.get());
                    repository.complete(next.get().id(), consumer.name());
                    return true;
                })) && delivered(consumer, selected.get());
            }
            // The row lock is released before an external HTTP call. External delivery is at-least-once;
            // its stable event ID must be used as the downstream idempotency key.
            var delivery = tx.execute(status -> repository.lockNext(consumer.name()).orElse(null));
            if (delivery == null) {
                return false;
            }
            selected.set(delivery);
            accept(consumer, delivery);
            tx.executeWithoutResult(status -> repository.complete(delivery.id(), consumer.name()));
            return delivered(consumer, delivery);
        } catch (Exception failure) {
            if (selected.get() != null) {
                var delivery = selected.get();
                try {
                    var retry = tx.execute(status -> repository.fail(delivery.id(), consumer.name(), failure.getClass().getSimpleName()));
                    if (retry != null && retry.isPresent()) {
                        var schedule = retry.get();
                        log.warn("event=outbox.delivery_failed consumer={} eventId={} attempts={} dead={} nextAttempt={} diagnostics={}",
                            consumer.name(), delivery.id(), schedule.attempts(), schedule.dead(), schedule.nextAttempt(),
                            FailureDiagnostics.describe(failure));
                    } else {
                        log.info("event=outbox.failure_ignored consumer={} eventId={} reason=already_completed",
                            consumer.name(), delivery.id());
                    }
                } catch (Exception retryFailure) {
                    log.error("event=outbox.retry_record_failed consumer={} eventId={} diagnostics={}",
                        consumer.name(), delivery.id(), FailureDiagnostics.describe(retryFailure));
                }
                metrics.counter("hotel.events.failed", "consumer", consumer.name()).increment();
            } else {
                log.error("event=outbox.poll_failed consumer={} diagnostics={}", consumer.name(), FailureDiagnostics.describe(failure));
            }
            return false;
        } finally {
            if (previous == null) {
                MDC.remove("traceId");
            } else {
                MDC.put("traceId", previous);
            }
        }
    }

    private void accept(EventConsumer consumer, EventDelivery delivery) {
        MDC.put("traceId", delivery.traceId());
        log.info("event=outbox.delivery_started consumer={} eventId={} type={}", consumer.name(), delivery.id(),
            delivery.type());
        consumer.accept(delivery.id(), delivery.type(), delivery.traceId(), json.read(delivery.payload(), ReservationSnapshot.class));
    }

    private boolean delivered(EventConsumer consumer, EventDelivery delivery) {
        metrics.counter("hotel.events.processed", "consumer", consumer.name()).increment();
        log.info("event=outbox.delivery_completed consumer={} eventId={}", consumer.name(), delivery.id());
        return true;
    }
}
