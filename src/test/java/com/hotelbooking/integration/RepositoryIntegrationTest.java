package com.hotelbooking.integration;

import com.hotelbooking.events.infrastructure.EventDeliveryRepository;
import com.hotelbooking.events.infrastructure.mapper.EventDeliveryEntityMapper;
import com.hotelbooking.hotel.infrastructure.HotelRepository;
import com.hotelbooking.hotel.infrastructure.mapper.HotelEntityMapper;
import com.hotelbooking.identity.infrastructure.StaffRepository;
import com.hotelbooking.identity.infrastructure.mapper.StaffEntityMapper;
import com.hotelbooking.inventory.infrastructure.AvailabilityQueryRepository;
import com.hotelbooking.inventory.infrastructure.InventoryCommandRepository;
import com.hotelbooking.inventory.infrastructure.mapper.RoomEntityMapper;
import com.hotelbooking.reservation.application.command.BookingData;
import com.hotelbooking.reservation.infrastructure.IdempotencyRepository;
import com.hotelbooking.reservation.infrastructure.ReservationRepository;
import com.hotelbooking.reservation.infrastructure.ReservationSnapshotRepository;
import com.hotelbooking.reservation.infrastructure.mapper.IdempotencyEntityMapper;
import com.hotelbooking.reservation.infrastructure.mapper.ReservationEntityMapper;
import com.hotelbooking.shared.domain.Stay;
import com.hotelbooking.shared.exception.BusinessException;
import com.hotelbooking.shared.persistence.JpaQueries;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.*;

@Tag("integration")
@Testcontainers
@DataJpaTest(properties = "spring.liquibase.contexts=test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    EventDeliveryRepository.class, EventDeliveryEntityMapper.class, StaffRepository.class, StaffEntityMapper.class,
    HotelRepository.class, HotelEntityMapper.class, InventoryCommandRepository.class, AvailabilityQueryRepository.class,
    RoomEntityMapper.class, JpaQueries.class, IdempotencyRepository.class, IdempotencyEntityMapper.class,
    ReservationRepository.class, ReservationSnapshotRepository.class, ReservationEntityMapper.class
})
class RepositoryIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.6-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", postgres::getJdbcUrl);
        properties.add("spring.datasource.username", postgres::getUsername);
        properties.add("spring.datasource.password", postgres::getPassword);
    }

    private static final UUID HOTEL = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_HOTEL = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID ROOM = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private final Stay stay = new Stay(LocalDate.of(2027, 1, 10), LocalDate.of(2027, 1, 12), 2, 0);

    @Autowired
    PlatformTransactionManager transactionManager;
    @Autowired
    InventoryCommandRepository inventory;
    @Autowired
    AvailabilityQueryRepository availability;
    @Autowired
    IdempotencyRepository idempotency;
    @Autowired
    ReservationRepository reservations;
    @Autowired
    ReservationSnapshotRepository snapshots;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    StaffRepository staff;
    @Autowired
    EventDeliveryRepository deliveries;
    @Autowired
    EntityManager entityManager;

    @Test
    void mapsSeededRoomAndKeepsTenantScope() {
        var room = inventory.room(HOTEL, ROOM);
        assertThat(room.hotelId()).isEqualTo(HOTEL);
        assertThat(room.capacity()).isEqualTo(2);
        assertThat(room.nightlyPrice()).isEqualByComparingTo("3200.00");
        assertThatThrownBy(() -> inventory.room(OTHER_HOTEL, ROOM)).isInstanceOf(BusinessException.class);
    }

    @Test
    void storesAndReadsTheIdempotencyResponse() {
        String hash = "a".repeat(64);
        assertThat(idempotency.lock(HOTEL, "repository-key", hash).response()).isNull();
        idempotency.complete(HOTEL, "repository-key", "{\"status\":\"CONFIRMED\"}");
        var saved = idempotency.lock(HOTEL, "repository-key", hash);
        assertThat(saved.hash()).isEqualTo(hash);
        assertThat(saved.response()).contains("CONFIRMED");
    }

    @Test
    void mapsReservationDatesConsentAndNullableCheckout() {
        UUID id = UUID.randomUUID();
        var request = new BookingData(ROOM, stay.checkIn(), stay.checkOut(), 2, 0, "Deniz", "Test",
            "d@example.com", "+905551234567", true, false, null);
        reservations.insert(id, HOTEL, "RT-REPOSITORY", request, new BigDecimal("6400.00"));
        var state = reservations.lock(HOTEL, id);
        var snapshot = snapshots.get(HOTEL, id);
        assertThat(state.checkIn()).isEqualTo(stay.checkIn());
        assertThat(snapshot.id()).isEqualTo(id);
        assertThat(snapshot.hotelId()).isEqualTo(HOTEL);
        assertThat(snapshot.consentAt()).isNotNull();
        assertThat(snapshot.checkedOutAt()).isNull();
        assertThat(snapshot.version()).isEqualTo(1);
        assertThat(reservations.roomId(HOTEL, id)).isEqualTo(ROOM);
        assertThat(availability.version(HOTEL)).isEqualTo(1);
    }

    @Test
    void inventoryUsesCheckoutExclusiveNightsAndReleasesExactlyThoseNights() {
        var room = inventory.room(HOTEL, ROOM);
        inventory.reserve(room, stay);
        assertThat(jdbc.queryForList("SELECT stay_date FROM room_inventory ORDER BY stay_date", LocalDate.class))
            .containsExactly(stay.checkIn(), stay.checkIn().plusDays(1));
        assertThat(availability.sold(HOTEL, ROOM, stay)).isEqualTo(1);
        inventory.release(HOTEL, ROOM, stay.checkIn(), stay.checkOut());
        assertThat(availability.sold(HOTEL, ROOM, stay)).isZero();
    }
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void inventoryLocksOnlyTheOccupiedDatesEvenWhenNightRowsAreCreatedConcurrently() throws Exception {
        var reserved = new CountDownLatch(1);
        var finish = new CountDownLatch(1);
        try (var pool = Executors.newSingleThreadExecutor()) {
            var holding = pool.submit(() -> new TransactionTemplate(transactionManager).execute(status -> {
                status.setRollbackOnly();
                inventory.reserve(inventory.room(HOTEL, ROOM), stay);
                reserved.countDown();
                try {
                    if (!finish.await(15, TimeUnit.SECONDS)) throw new IllegalStateException("Test lock was not released");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
                return null;
            }));
            try {
                assertThat(reserved.await(10, TimeUnit.SECONDS)).isTrue();
                var independent = new TransactionTemplate(transactionManager);
                independent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                independent.executeWithoutResult(status -> {
                    status.setRollbackOnly();
                    jdbc.execute("SET LOCAL lock_timeout = '500ms'");
                    // Starts on checkout: no occupied night is shared with the held transaction.
                    var next = new Stay(stay.checkOut(), stay.checkOut().plusDays(2), 2, 0);
                    inventory.reserve(inventory.room(HOTEL, ROOM), next);
                });
                assertThatThrownBy(() -> independent.executeWithoutResult(status -> {
                    status.setRollbackOnly();
                    jdbc.execute("SET LOCAL lock_timeout = '500ms'");
                    inventory.reserve(inventory.room(HOTEL, ROOM), stay);
                })).isInstanceOf(DataAccessException.class);
            } finally {
                finish.countDown();
            }
            holding.get(10, TimeUnit.SECONDS);
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void simultaneousBookingsConsumeOneUnitEachAndNeverExceedNightCapacity() throws Exception {
        var dates = new Stay(LocalDate.of(2027, 5, 10), LocalDate.of(2027, 5, 12), 2, 0);
        for (var date : dates.dates()) {
            jdbc.update("INSERT INTO room_inventory(hotel_id,room_type_id,stay_date,capacity,sold) VALUES (?,?,?,?,0)",
                HOTEL, ROOM, date, 2);
        }
        var ready = new CountDownLatch(3);
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(3)) {
            var jobs = java.util.stream.IntStream.range(0, 3).mapToObj(index -> pool.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Booking race did not start");
                try {
                    new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                        inventory.reserve(inventory.room(HOTEL, ROOM), dates));
                    return true;
                } catch (BusinessException soldOut) {
                    return false;
                }
            })).toList();
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            var results = new java.util.ArrayList<Boolean>();
            for (var job : jobs) results.add(job.get(10, TimeUnit.SECONDS));
            assertThat(results).containsExactlyInAnyOrder(true, true, false);
            assertThat(jdbc.queryForList("SELECT sold FROM room_inventory WHERE hotel_id=? AND room_type_id=? AND stay_date>=? AND stay_date<? ORDER BY stay_date",
                Integer.class, HOTEL, ROOM, dates.checkIn(), dates.checkOut())).isEqualTo(List.of(2, 2));
        } finally {
            start.countDown();
            jdbc.update("DELETE FROM room_inventory WHERE hotel_id=? AND room_type_id=? AND stay_date>=? AND stay_date<?",
                HOTEL, ROOM, dates.checkIn(), dates.checkOut());
        }
    }

    @Test
    void demoPasswordSyncUpdatesExistingHashWithoutMovingTheAccountToAnotherHotel() {
        staff.saveDemoAccount("lagos", "old-hash", HOTEL);
        staff.saveDemoAccount("lagos", "new-hash", HOTEL);
        assertThat(staff.credentials("lagos").orElseThrow().passwordHash()).isEqualTo("new-hash");
        assertThatThrownBy(() -> staff.saveDemoAccount("lagos", "foreign-hash", OTHER_HOTEL))
            .isInstanceOf(IllegalStateException.class);
        assertThat(staff.hotel("lagos")).isEqualTo(HOTEL);
        assertThat(staff.credentials("lagos").orElseThrow().passwordHash()).isEqualTo("new-hash");
    }

    @Test
    void retryReturnsItsScheduleAndLateFailureCannotReopenCompletedDelivery() {
        UUID reservation = UUID.randomUUID();
        UUID event = UUID.randomUUID();
        var booking = new BookingData(ROOM, stay.checkIn(), stay.checkOut(), 2, 0,
            "Retry", "Guest", "retry@example.com", "+905551234567", true, false, null);
        reservations.insert(reservation, HOTEL, "RT-RETRY-SQL", booking, new BigDecimal("6400.00"));
        // JdbcTemplate does not flush pending JPA inserts before checking foreign keys.
        entityManager.flush();
        jdbc.update("""
            INSERT INTO outbox_event(id,hotel_id,reservation_id,aggregate_version,event_type,payload,trace_id)
            VALUES (?,?,?,1,'ReservationCreated','{}'::jsonb,'retry-test-trace')
            """,
            event, HOTEL, reservation);
        jdbc.update("INSERT INTO event_delivery(event_id,consumer) VALUES (?,'PMS')", event);
        var first = deliveries.fail(event, "PMS", "Timeout").orElseThrow();
        assertThat(first.attempts()).isEqualTo(1);
        assertThat(first.dead()).isFalse();
        assertThat(first.nextAttempt()).isNotNull();
        jdbc.update("UPDATE event_delivery SET attempts=9 WHERE event_id=?", event);
        entityManager.clear();
        assertThat(deliveries.fail(event, "PMS", "Timeout").orElseThrow().dead()).isTrue();
        deliveries.complete(event, "PMS");
        assertThat(deliveries.fail(event, "PMS", "LateFailure")).isEmpty();
        assertThat(jdbc.queryForObject("SELECT dead FROM event_delivery WHERE event_id=?", Boolean.class, event)).isFalse();
        assertThat(jdbc.queryForObject("SELECT processed_at IS NOT NULL FROM event_delivery WHERE event_id=?",
            Boolean.class, event)).isTrue();
    }

    @Test
    void reservationStatusKeepsEventVersionAndOptimisticLockVersionSeparate() {
        UUID id = UUID.randomUUID();
        var request = new BookingData(ROOM, stay.checkIn(), stay.checkOut(), 2, 0,
            "Version", "Guest", "version@example.com", "+905551234567", true, false, null);
        reservations.insert(id, HOTEL, "RT-VERSION", request, new BigDecimal("6400.00"));
        assertThat(snapshots.get(HOTEL, id).version()).isEqualTo(1);
        reservations.changeStatus(HOTEL, id, com.hotelbooking.reservation.domain.ReservationStatus.CHECKED_IN);
        assertThat(snapshots.get(HOTEL, id).version()).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT lock_version FROM reservation WHERE id=?", Long.class, id)).isEqualTo(1L);
    }

    @Test
    void dailyServicesPersistSeparateDaysRescheduleOneTransferAndKeepTenantAudit() {
        UUID id = UUID.randomUUID();
        reservations.insert(id, HOTEL, "RT-DAILY-SQL", new BookingData(ROOM, stay.checkIn(), stay.checkOut(),
            2, 0,
            "Daily", "Guest", "daily@example.com", "+905551234567", true, false, null), new BigDecimal("6400.00"));
        entityManager.flush();
        var json = new com.hotelbooking.shared.json.Json(new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules());
        var repository = new com.hotelbooking.pms.infrastructure.DailyServiceRepository(entityManager,
            new com.hotelbooking.pms.infrastructure.mapper.DailyServiceMapper(), json);
        var day1 = stay.checkIn().plusDays(1);
        var day2 = stay.checkOut();
        repository.save(HOTEL, new com.hotelbooking.pms.application.model.DailyServiceTask(id, "BREAKFAST",
            day1,
            com.hotelbooking.pms.domain.DailyServiceStatus.SERVED, null, "", "", "", "", 2, "staff", java.time.Instant.now()));
        repository.save(HOTEL, new com.hotelbooking.pms.application.model.DailyServiceTask(id, "BREAKFAST",
            day2,
            com.hotelbooking.pms.domain.DailyServiceStatus.PENDING, null, "", "", "", "", 2, "staff", java.time.Instant.now()));
        assertThat(repository.tasks(HOTEL, java.util.List.of(id), day1)).extracting(t -> t.status().name()).containsExactly("SERVED");
        assertThat(repository.tasks(HOTEL, java.util.List.of(id), day2)).extracting(t -> t.status().name()).containsExactly("PENDING");
        repository.save(HOTEL, new com.hotelbooking.pms.application.model.DailyServiceTask(id, "TRANSFER",
            stay.checkIn(),
            com.hotelbooking.pms.domain.DailyServiceStatus.PLANNED, java.time.LocalTime.NOON, "Airport", "Hotel",
            "", "", 2, "staff", java.time.Instant.now()));
        repository.save(HOTEL, new com.hotelbooking.pms.application.model.DailyServiceTask(id, "TRANSFER",
            stay.checkIn().minusDays(1),
            com.hotelbooking.pms.domain.DailyServiceStatus.PLANNED, java.time.LocalTime.NOON, "Airport", "Hotel",
            "", "Rescheduled", 2, "staff", java.time.Instant.now()));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pms_daily_service WHERE reservation_id=? AND code='TRANSFER'",
            Long.class, id)).isEqualTo(1);
        assertThat(repository.history(HOTEL, id, "TRANSFER", stay.checkIn())).hasSize(2);
        assertThat(repository.history(HOTEL, id, "TRANSFER", stay.checkIn())).anyMatch(a -> a.afterState().contains("Rescheduled"));
        assertThat(repository.tasks(OTHER_HOTEL, java.util.List.of(id), day1)).isEmpty();
        assertThat(repository.history(OTHER_HOTEL, id, "BREAKFAST", day1)).isEmpty();
        assertThat(jdbc.queryForObject("SELECT total FROM reservation WHERE id=?", BigDecimal.class, id)).isEqualByComparingTo("6400.00");
    }

}
