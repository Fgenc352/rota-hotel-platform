package com.hotelbooking.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelbooking.contracts.reservation.ReservationSnapshot;
import com.hotelbooking.events.application.EventConsumer;
import com.hotelbooking.events.application.EventDeliveryProcessor;
import com.hotelbooking.events.application.model.EventDelivery;
import com.hotelbooking.events.application.model.RetrySchedule;
import com.hotelbooking.events.application.port.EventDeliveries;
import com.hotelbooking.events.config.OutboxProperties;
import com.hotelbooking.events.infrastructure.OutboxScheduler;
import com.hotelbooking.events.infrastructure.OutboxWorker;
import com.hotelbooking.shared.json.Json;
import com.hotelbooking.support.TestTransactionManager;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.*;

@Tag("unit")
class OutboxDeliveryTest {
    private final Json json = new Json(new ObjectMapper().findAndRegisterModules());

    private EventDelivery event() {
        var today = LocalDate.of(2026, 11, 1);
        var snapshot = new ReservationSnapshot(UUID.randomUUID(), UUID.randomUUID(), "RT-test", today, today.plusDays(1),
            "Private", "Guest", "private@example.com", "+905551234567", false, Instant.now(), "CONFIRMED", BigDecimal.ONE,
            1, null);
        return new EventDelivery(UUID.randomUUID(), "ReservationCreated", "outbox-test-trace", json.write(snapshot));
    }

    private class Deliveries implements EventDeliveries {
        private final EventDelivery event = event();
        private final Set<String> completed = ConcurrentHashMap.newKeySet();
        private int failures;
        public Optional<EventDelivery> lockNext(String name) {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            return completed.contains(name) ? Optional.empty() : Optional.of(event);
        }
        public void complete(UUID id, String name) {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            completed.add(name);
        }
        public Optional<RetrySchedule> fail(UUID id, String name, String error) {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            failures++;
            return Optional.of(new RetrySchedule(failures, false, Instant.now().plusSeconds(2)));
        }
    }

    private EventDeliveryProcessor processor(Deliveries deliveries) {
        return new EventDeliveryProcessor(deliveries, new TransactionTemplate(new TestTransactionManager()),
            json, new SimpleMeterRegistry());
    }

    private EventConsumer consumer(String name, boolean local, Consumer<ReservationSnapshot> action) {
        return new EventConsumer() {
            public String name() {
                return name;
            }
            public boolean requiresLocalTransaction() {
                return local;
            }
            public void accept(UUID id, String type, String trace, ReservationSnapshot snapshot) {
                action.accept(snapshot);
            }
        };
    }

    @Test
    void localProjectionAndAcknowledgementRunInsideTheSameTransaction() {
        var deliveries = new Deliveries();
        var consumer = consumer("CRM", true, snapshot -> assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue());
        var processor = processor(deliveries);
        assertThat(processor.processNext(consumer)).isTrue();
        assertThat(processor.processNext(consumer)).isFalse();
        assertThat(deliveries.completed).containsExactly("CRM");
    }

    @Test
    void externalCallRunsOutsideDatabaseTransactionButAcknowledgementIsTransactional() {
        var deliveries = new Deliveries();
        var consumer = consumer("PMS", false, snapshot -> assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse());
        assertThat(processor(deliveries).processNext(consumer)).isTrue();
        assertThat(deliveries.completed).containsExactly("PMS");
    }

    @Test
    void failedExternalDeliveryRemainsPendingAndCanBeRetried() {
        var deliveries = new Deliveries();
        var processor = processor(deliveries);
        assertThat(processor.processNext(consumer("PMS", false, snapshot -> {
            throw new IllegalStateException("private upstream error");
        }))).isFalse();
        assertThat(deliveries.failures).isEqualTo(1);
        assertThat(deliveries.completed).isEmpty();
        assertThat(processor.processNext(consumer("PMS", false, snapshot -> {
        }))).isTrue();
    }

    @Test
    void slowExternalConsumerDoesNotBlockCrmScheduledWork() throws Exception {
        var enteredPms = new CountDownLatch(1);
        var releasePms = new CountDownLatch(1);
        var crmProgress = new CountDownLatch(1);
        var consumers = List.of(consumer("PMS", false, snapshot -> {
            enteredPms.countDown();
            try {
                assertThat(releasePms.await(5, TimeUnit.SECONDS)).isTrue();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }), consumer("CRM", true, snapshot -> crmProgress.countDown()));
        var properties = new OutboxProperties(1, Duration.ofSeconds(30));
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.initialize();
        var jobs = new OutboxScheduler(new OutboxWorker(processor(new Deliveries()), consumers, properties),
            consumers, properties, scheduler);
        try {
            jobs.start();
            assertThat(enteredPms.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(crmProgress.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(releasePms.getCount()).isEqualTo(1);
        } finally {
            releasePms.countDown();
            jobs.destroy();
            scheduler.shutdown();
        }
    }
}
