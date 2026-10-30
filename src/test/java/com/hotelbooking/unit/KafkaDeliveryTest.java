package com.hotelbooking.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelbooking.contracts.reservation.ReservationSnapshot;
import com.hotelbooking.events.application.model.BrokerReservationEvent;
import com.hotelbooking.events.application.port.PmsEventInbox;
import com.hotelbooking.events.config.MessagingProperties;
import com.hotelbooking.events.infrastructure.OutboxEventRepository;
import com.hotelbooking.events.infrastructure.entity.EventDeliveryEntity;
import com.hotelbooking.events.infrastructure.kafka.KafkaPmsInboxListener;
import com.hotelbooking.events.infrastructure.kafka.KafkaReservationPublisher;
import com.hotelbooking.events.infrastructure.kafka.KafkaTopicProvisioner;
import com.hotelbooking.shared.json.Json;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("unit")
class KafkaDeliveryTest {
    private final Json json = new Json(new ObjectMapper().findAndRegisterModules());
    private final UUID eventId = UUID.randomUUID();
    private final ReservationSnapshot snapshot = new ReservationSnapshot(UUID.randomUUID(), UUID.randomUUID(),
        "RT-test",
        LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 9), "Private", "Guest", "private@example.com", "+905551234567",
        false, Instant.now(), "CONFIRMED", BigDecimal.TEN, 1, null);

    private MessagingProperties properties(MessagingProperties.Transport transport) {
        return new MessagingProperties(transport, "hotel.events", "hotel.events.DLT", "pms-test", 3, 1, Duration.ofMillis(30));
    }

    @Test
    void outboxKeepsTheExistingDirectPathAndUsesASeparateKafkaStageWhenSelected() {
        for (var transport : MessagingProperties.Transport.values()) {
            var em = mock(EntityManager.class);
            new OutboxEventRepository(em, json, properties(transport))
                .append(eventId, snapshot.hotelId(), snapshot.id(), "ReservationCreated", snapshot, "trace-test");
            var persisted = ArgumentCaptor.forClass(Object.class);
            verify(em, times(3)).persist(persisted.capture());
            assertThat(persisted.getAllValues().stream().filter(EventDeliveryEntity.class::isInstance)
                .map(EventDeliveryEntity.class::cast).map(value -> value.getId().getConsumer()))
                .containsExactly("CRM", transport == MessagingProperties.Transport.KAFKA ? "KAFKA" : "PMS");
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void publisherWaitsForBrokerAcknowledgementAndSendsNoGuestContactData() {
        KafkaTemplate<Object, Object> kafka = mock(KafkaTemplate.class);
        SendResult<Object, Object> result = mock(SendResult.class);
        var metadata = new RecordMetadata(new TopicPartition("hotel.events", 0), 0L, 0, 0L, -1, -1);
        when(result.getRecordMetadata()).thenReturn(metadata);
        when(kafka.send(anyString(), any(), any())).thenReturn(CompletableFuture.completedFuture(result));
        var publisher = new KafkaReservationPublisher(kafka, json, properties(MessagingProperties.Transport.KAFKA),
            mock(KafkaTopicProvisioner.class));
        publisher.accept(eventId, "ReservationCreated", "trace-test", snapshot);
        var body = ArgumentCaptor.forClass(Object.class);
        verify(kafka).send(eq("hotel.events"), eq(snapshot.id().toString()), body.capture());
        assertThat(body.getValue().toString()).doesNotContain("private@example.com", "+905551234567", "firstName",
            "lastName");
        assertThat(json.read(body.getValue().toString(), BrokerReservationEvent.class).eventId()).isEqualTo(eventId);
    }

    @Test
    @SuppressWarnings("unchecked")
    void timeoutDoesNotAcknowledgeAnUnconfirmedPublish() {
        KafkaTemplate<Object, Object> kafka = mock(KafkaTemplate.class);
        when(kafka.send(anyString(), any(), any())).thenReturn(new CompletableFuture<>());
        var publisher = new KafkaReservationPublisher(kafka, json, properties(MessagingProperties.Transport.KAFKA),
            mock(KafkaTopicProvisioner.class));
        assertThatThrownBy(() -> publisher.accept(eventId, "ReservationCreated", "trace-test", snapshot))
            .isInstanceOf(IllegalStateException.class).hasRootCauseInstanceOf(TimeoutException.class);
    }

    @Test
    void listenerStagesOnlyValidatedMetadataAndPropagatesPersistenceFailures() {
        var inbox = mock(PmsEventInbox.class);
        var listener = new KafkaPmsInboxListener(json, inbox);
        var message = new BrokerReservationEvent(eventId, snapshot.hotelId(), snapshot.id(), 1, "ReservationCreated",
            "trace-test");
        listener.receive(json.write(message));
        verify(inbox).stage(message);
        assertThatThrownBy(() -> listener.receive("invalid json")).isInstanceOf(IllegalStateException.class);
        when(inbox.stage(message)).thenThrow(new IllegalStateException("database unavailable"));
        assertThatThrownBy(() -> listener.receive(json.write(message))).isInstanceOf(IllegalStateException.class);
    }
    @Test
    void topicProvisioningRetriesAfterLateBrokerStartupAndStopsAfterSuccess() {
        var admin = mock(org.springframework.kafka.core.KafkaAdminOperations.class);
        doThrow(new IllegalStateException("Broker offline")).doNothing().when(admin).createOrModifyTopics(any(),
            any());
        var topics = new KafkaTopicProvisioner(admin, properties(MessagingProperties.Transport.KAFKA));
        assertThatThrownBy(topics::ensureReady).isInstanceOf(IllegalStateException.class);
        topics.ensureReady();
        topics.ensureReady();
        verify(admin, times(2)).createOrModifyTopics(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void brokerNotReadyDoesNotSendOrAcknowledgeTheEvent() {
        KafkaTemplate<Object, Object> kafka = mock(KafkaTemplate.class);
        var topics = mock(KafkaTopicProvisioner.class);
        doThrow(new IllegalStateException("Broker offline")).when(topics).ensureReady();
        var publisher = new KafkaReservationPublisher(kafka, json, properties(MessagingProperties.Transport.KAFKA),
            topics);
        assertThatThrownBy(() -> publisher.accept(eventId, "ReservationCreated", "trace", snapshot)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(kafka);
    }

}
