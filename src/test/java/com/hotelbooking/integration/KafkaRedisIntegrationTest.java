package com.hotelbooking.integration;

import com.hotelbooking.contracts.reservation.ReservationSnapshot;
import com.hotelbooking.events.application.EventDeliveryProcessor;
import com.hotelbooking.events.application.PmsGateway;
import com.hotelbooking.events.application.model.BrokerReservationEvent;
import com.hotelbooking.events.application.port.PmsEventInbox;
import com.hotelbooking.events.config.MessagingProperties;
import com.hotelbooking.events.infrastructure.PmsEventConsumer;
import com.hotelbooking.events.infrastructure.kafka.KafkaReservationPublisher;
import com.hotelbooking.inventory.application.model.Offer;
import com.hotelbooking.inventory.application.port.AvailabilityResults;
import com.hotelbooking.inventory.config.AvailabilityCacheProperties;
import com.hotelbooking.inventory.infrastructure.RedisAvailabilityCache;
import com.hotelbooking.reservation.application.command.BookingData;
import com.hotelbooking.reservation.application.command.CreateReservationHandler;
import com.hotelbooking.shared.domain.Stay;
import com.hotelbooking.shared.json.Json;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("integration")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(properties = {
    "app.worker-enabled=false", "spring.liquibase.contexts=test"
})
class KafkaRedisIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.6-alpine");
    @Container
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:4.1.2"));
    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.4")).withExposedPorts(6379);

    @DynamicPropertySource
    static void infrastructure(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", postgres::getJdbcUrl);
        properties.add("spring.datasource.username", postgres::getUsername);
        properties.add("spring.datasource.password", postgres::getPassword);
        properties.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        properties.add("spring.data.redis.host", redis::getHost);
        properties.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired CreateReservationHandler bookings;
    @Autowired com.hotelbooking.reservation.application.command.ChangeReservationStatusHandler statuses;
    @Autowired EventDeliveryProcessor processor;
    @Autowired KafkaReservationPublisher publisher;
    @Autowired PmsEventConsumer pmsConsumer;
    @Autowired PmsEventInbox inbox;
    @Autowired JdbcTemplate jdbc;
    @Autowired Json json;
    @Autowired MessagingProperties messaging;
    @Autowired KafkaTemplate<Object, Object> template;
    @Autowired AvailabilityResults cache;
    @Autowired com.hotelbooking.inventory.application.query.SearchAvailabilityHandler availabilitySearch;
    @Autowired AvailabilityCacheProperties cacheProperties;
    @Autowired StringRedisTemplate redisTemplate;
    @MockitoBean PmsGateway pms;

    @Test
    void realBrokerStagesDurablePmsDeliveryAndExistingRetryRemainsIdempotent() {
        LocalDate start = LocalDate.now(ZoneId.of("Europe/Istanbul")).plusDays(40);
        var input = new BookingData(UUID.fromString("20000000-0000-0000-0000-000000000001"), start, start.plusDays(2),
            2, 0, "Kafka", "Guest", "kafka@example.com", "+905551111111", true, false, null, List.of());
        var receipt = bookings.handle(new CreateReservationHandler.Input("lagos-beach", UUID.randomUUID().toString(),
            input));
        UUID event = jdbc.queryForObject("SELECT id FROM outbox_event WHERE reservation_id=?", UUID.class,
            receipt.id());
        assertThat(jdbc.queryForList("SELECT consumer FROM event_delivery WHERE event_id=? ORDER BY consumer",
            String.class, event))
            .containsExactly("CRM", "KAFKA");
        assertThat(processor.processNext(publisher)).isTrue();
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
            assertThat(jdbc.queryForObject("SELECT count(*) FROM event_delivery WHERE event_id=? AND consumer='PMS'",
            Integer.class, event)).isEqualTo(1));
        verifyNoInteractions(pms);
        doThrow(new IllegalStateException("PMS offline")).doNothing().when(pms).deliver(eq(event), anyString(),
            any());
        assertThat(processor.processNext(pmsConsumer)).isFalse();
        assertThat(jdbc.queryForObject("SELECT attempts FROM event_delivery WHERE event_id=? AND consumer='PMS'",
            Integer.class, event)).isEqualTo(1);
        jdbc.update("UPDATE event_delivery SET next_attempt=now() WHERE event_id=? AND consumer='PMS'", event);
        assertThat(processor.processNext(pmsConsumer)).isTrue();
        var snapshot = json.read(jdbc.queryForObject("SELECT payload::text FROM outbox_event WHERE id=?", String.class,
            event), ReservationSnapshot.class);
        var envelope = new BrokerReservationEvent(event, snapshot.hotelId(), receipt.id(), snapshot.version(),
            "ReservationCreated",
            jdbc.queryForObject("SELECT trace_id FROM outbox_event WHERE id=?", String.class, event));
        assertThat(inbox.stage(envelope)).isFalse();
        assertThat(jdbc.queryForObject("SELECT processed_at IS NOT NULL FROM event_delivery WHERE event_id=? AND consumer='PMS'",
            Boolean.class, event)).isTrue();
        verify(pms, times(2)).deliver(eq(event), anyString(), any());
        assertThatThrownBy(() -> inbox.stage(new BrokerReservationEvent(event, UUID.randomUUID(), receipt.id(),
            1, "ReservationCreated", envelope.traceId())))
            .isInstanceOf(org.springframework.dao.InvalidDataAccessApiUsageException.class)
            .hasRootCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Broker event does not match the durable outbox");
    }

    @Test
    void malformedRecordGoesToDeadLetterTopicRatherThanBecomingAPmsDelivery() throws Exception {
        Map<String, Object> settings = Map.of(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
            ConsumerConfig.GROUP_ID_CONFIG, "dlt-check-" + UUID.randomUUID(),
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        try (var consumer = new KafkaConsumer<String, String>(settings)) {
            consumer.subscribe(List.of(messaging.deadLetterTopic()));
            template.send(messaging.topic(), "bad-envelope", "not-json").get(10, TimeUnit.SECONDS);
            await().atMost(Duration.ofSeconds(30)).until(() -> {
                for (var record : consumer.poll(Duration.ofMillis(500))) {
                    if (record.value().equals("not-json")) return true;
                }
                return false;
            });
        }
    }

    @Test
    void realRedisSharesOffersAcrossInstancesAndUsesExpiringVersionedKeys() {
        var hotel = UUID.randomUUID();
        var stay = new Stay(LocalDate.of(2027, 1, 5), LocalDate.of(2027, 1, 7), 2, 0);
        var offers = List.of(new Offer(UUID.randomUUID(), "Room", 2, 1, BigDecimal.TEN, new BigDecimal("20.00"),
            "TRY", List.of(), 1, BigDecimal.TEN, BigDecimal.ONE));
        cache.put(hotel, stay, 7, offers);
        var otherInstance = new RedisAvailabilityCache(redisTemplate, json, cacheProperties);
        assertThat(otherInstance.get(hotel, stay, 7)).isEqualTo(offers);
        assertThat(otherInstance.get(hotel, stay, 8)).isNull();
        assertThat(otherInstance.get(UUID.randomUUID(), stay, 7)).isNull();
        var keys = redisTemplate.keys(cacheProperties.namespace() + ":" + hotel + ":*");
        assertThat(keys).hasSize(1);
        String key = keys.iterator().next();
        assertThat(redisTemplate.getExpire(key, TimeUnit.MILLISECONDS)).isPositive().isLessThanOrEqualTo(cacheProperties.ttl().toMillis());
        redisTemplate.delete(key);
        assertThat(cache.get(hotel, stay, 7)).isNull();
    }
    @Test
    void reservationStatusVersionsReachPmsThroughRealKafkaInOrder() {
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Istanbul"));
        var input = new BookingData(UUID.fromString("20000000-0000-0000-0000-000000000001"), today, today.plusDays(2),
            2, 0, "Kafka", "Lifecycle", "lifecycle@example.com", "+905551111112", true, false, null, List.of());
        var receipt = bookings.handle(new CreateReservationHandler.Input("lagos-beach", UUID.randomUUID().toString(),
            input));
        statuses.handle(new com.hotelbooking.reservation.application.command.ChangeReservationStatusHandler.Input(
            UUID.fromString("10000000-0000-0000-0000-000000000001"), receipt.id(), com.hotelbooking.reservation.domain.ReservationStatus.CHECKED_IN));
        statuses.handle(new com.hotelbooking.reservation.application.command.ChangeReservationStatusHandler.Input(
            UUID.fromString("10000000-0000-0000-0000-000000000001"), receipt.id(), com.hotelbooking.reservation.domain.ReservationStatus.CHECKED_OUT));
        for (int i = 0; i<3; i++) assertThat(processor.processNext(publisher)).isTrue();
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> assertThat(jdbc.queryForObject(
            "SELECT count(*) FROM event_delivery d JOIN outbox_event e ON e.id=d.event_id WHERE e.reservation_id=? AND d.consumer='PMS'",
            Long.class, receipt.id())).isEqualTo(3));
        for (int i = 0; i<3; i++) assertThat(processor.processNext(pmsConsumer)).isTrue();
        var snapshots = org.mockito.ArgumentCaptor.forClass(ReservationSnapshot.class);
        verify(pms, times(3)).deliver(any(), anyString(), snapshots.capture());
        assertThat(snapshots.getAllValues()).extracting(ReservationSnapshot::status).containsExactly("CONFIRMED",
            "CHECKED_IN", "CHECKED_OUT");
        assertThat(snapshots.getAllValues()).extracting(ReservationSnapshot::version).containsExactly(1, 2,
            3);
    }

    @Test
    void brokerOutageKeepsTheOutboxAndResumesWithoutDuplicatingPmsDelivery() {
        LocalDate date = LocalDate.now(ZoneId.of("Europe/Istanbul")).plusDays(50);
        var input = new BookingData(UUID.fromString("20000000-0000-0000-0000-000000000001"), date, date.plusDays(1),
            2, 0, "Broker", "Recovery", "recovery@example.com", "+905551111113", true, false, null, List.of());
        var receipt = bookings.handle(new CreateReservationHandler.Input("lagos-beach", UUID.randomUUID().toString(),
            input));
        UUID event = jdbc.queryForObject("SELECT id FROM outbox_event WHERE reservation_id=?", UUID.class,
            receipt.id());
        var docker = org.testcontainers.DockerClientFactory.instance().client();
        docker.pauseContainerCmd(kafka.getContainerId()).exec();
        try {
            assertThat(processor.processNext(publisher)).isFalse();
            assertThat(jdbc.queryForObject("SELECT processed_at IS NULL FROM event_delivery WHERE event_id=? AND consumer='KAFKA'",
                Boolean.class, event)).isTrue();
        } finally {
            docker.unpauseContainerCmd(kafka.getContainerId()).exec();
        }
        await().atMost(Duration.ofSeconds(30)).until(() -> {
            jdbc.update("UPDATE event_delivery SET next_attempt=now() WHERE event_id=? AND consumer='KAFKA'",
                event);
            return processor.processNext(publisher);
        });
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> assertThat(jdbc.queryForObject(
            "SELECT count(*) FROM event_delivery WHERE event_id=? AND consumer='PMS'", Long.class, event)).isEqualTo(1));
        assertThat(processor.processNext(pmsConsumer)).isTrue();
        verify(pms).deliver(eq(event), anyString(), any());
    }

    @Test
    void realRedisOutageFallsBackToDatabaseAndRecovers() {
        LocalDate date = LocalDate.now(ZoneId.of("Europe/Istanbul")).plusDays(70);
        var input = new com.hotelbooking.inventory.application.query.SearchAvailabilityHandler.Input("lagos-beach",
            date, date.plusDays(2), 2, 0);
        var docker = org.testcontainers.DockerClientFactory.instance().client();
        docker.pauseContainerCmd(redis.getContainerId()).exec();
        try {
            assertThat(availabilitySearch.handle(input)).isNotEmpty();
        } finally {
            docker.unpauseContainerCmd(redis.getContainerId()).exec();
        }
        UUID hotel = UUID.randomUUID();
        var stay = new Stay(date, date.plusDays(1), 2, 0);
        var offers = List.of(new Offer(UUID.randomUUID(), "Recovered", 2, 1, BigDecimal.TEN, BigDecimal.TEN,
            "TRY", List.of(), 1, BigDecimal.TEN, BigDecimal.ONE));
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            cache.put(hotel, stay, 1, offers);
            assertThat(cache.get(hotel, stay, 1)).isEqualTo(offers);
        });
    }

}
