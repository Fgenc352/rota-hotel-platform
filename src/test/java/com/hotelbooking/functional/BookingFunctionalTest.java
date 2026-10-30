package com.hotelbooking.functional;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.hotelbooking.events.application.EventDeliveryProcessor;
import com.hotelbooking.events.config.OutboxProperties;
import com.hotelbooking.events.config.PmsProperties;
import com.hotelbooking.events.infrastructure.HttpPmsGateway;
import com.hotelbooking.events.infrastructure.OutboxWorker;
import com.hotelbooking.events.infrastructure.PmsEventConsumer;
import com.hotelbooking.guest.application.command.ApplyReservationProjectionHandler;
import com.hotelbooking.guest.infrastructure.CrmEventConsumer;
import com.hotelbooking.reservation.api.dto.BookingRequest;
import com.hotelbooking.reservation.application.command.ChangeReservationStatusHandler;
import com.hotelbooking.reservation.application.model.ReservationReceipt;
import com.hotelbooking.shared.json.Json;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.web.ServletTestExecutionListener;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

import static org.assertj.core.api.Assertions.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@Tag("functional")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestExecutionListeners({
    ServletTestExecutionListener.class, DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class
})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "app.worker-enabled=false", "spring.liquibase.contexts=test",
    "app.messaging.transport=direct", "app.availability-cache.provider=memory",
    "management.health.redis.enabled=false"
})
class BookingFunctionalTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.6-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    TestRestTemplate http;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ChangeReservationStatusHandler service;

    @Autowired
    EventDeliveryProcessor processor;

    @Autowired
    ApplyReservationProjectionHandler projection;

    @Autowired
    Json json;


    static final UUID HOTEL = UUID.fromString("10000000-0000-0000-0000-000000000001");
    static final UUID ROOM = UUID.fromString("20000000-0000-0000-0000-000000000001");
    LocalDate in = LocalDate.now(ZoneId.of("Europe/Istanbul")).plusDays(10);

    private final WireMockServer pmsHttp = new WireMockServer(wireMockConfig()
        .dynamicPort().usingFilesUnderClasspath("wiremock/pms"));

    @AfterEach
    void stopPms() {
        if (pmsHttp.isRunning()) {
            pmsHttp.stop();
        }
    }

    @BeforeEach
    void reset() {
        pmsHttp.start();
        pmsHttp.setScenarioState("pms-delivery", "unavailable");
        jdbc.execute("TRUNCATE guest_segment,event_delivery,outbox_event,guest_tag,guest_note,guest_identifier,crm_reservation,guest,funnel_event,idempotency_record,reservation_night,reservation,room_inventory CASCADE");
        jdbc.update("UPDATE pms_room SET housekeeping='CLEAN'");
        jdbc.update("UPDATE room_type SET rooms=20,min_nights=1,weekend_multiplier=1 WHERE id=?", ROOM);
    }
    BookingRequest request(String email) {
        return new BookingRequest(ROOM, in, in.plusDays(2), 2, 0, "Deniz", "Test", email, "+905551234567",
            true, true, UUID.randomUUID());
    }
    ResponseEntity<ReservationReceipt> book(String key, BookingRequest body) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", key);
        var response = http.exchange("/api/public/hotels/lagos-beach/reservations", HttpMethod.POST,
            new HttpEntity<>(body, headers), String.class);
        return ResponseEntity.status(response.getStatusCode()).body(response.getStatusCode().is2xxSuccessful()?json.read(response.getBody(),
            ReservationReceipt.class):null);
    }

    private OutboxWorker worker() {
        var pms = new PmsProperties(URI.create(pmsHttp.baseUrl()), Duration.ofSeconds(2), Duration.ofSeconds(3));
        return new OutboxWorker(processor, List.of(new CrmEventConsumer(projection),
            new PmsEventConsumer(new HttpPmsGateway(json, pms, HttpClient.newBuilder()
            .connectTimeout(pms.connectTimeout()).build(), new com.hotelbooking.events.application.mapper.PmsMessageMapper()))),
            new OutboxProperties(20, Duration.ofMillis(500)));
    }

    @Test
    void pmsFailureDoesNotBlockBookingOrCrmAndRetryAcknowledgesTheSameEvent() {
        pmsHttp.setScenarioState("pms-delivery", "recovering");
        var response = book("wiremock-recovery-booking", request("wiremock@example.com"));
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        var worker = worker();
        worker.run();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM crm_reservation", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT attempts FROM event_delivery WHERE consumer='PMS'", Integer.class))
            .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM event_delivery WHERE consumer='PMS' AND processed_at IS NULL", Long.class))
            .isEqualTo(1);
        // Advance only the test fixture's retry eligibility, avoiding a real backoff sleep.
        jdbc.update("UPDATE event_delivery SET next_attempt=now() WHERE consumer='PMS'");
        worker.run();
        worker.run();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM event_delivery WHERE consumer='PMS' AND processed_at IS NOT NULL", Long.class))
            .isEqualTo(1);
        UUID event = jdbc.queryForObject("SELECT id FROM outbox_event WHERE reservation_id=?", UUID.class,
            response.getBody().id());
        pmsHttp.verify(2, postRequestedFor(urlEqualTo("/reservations"))
            .withHeader("Idempotency-Key", equalTo(event.toString()))
            .withRequestBody(matchingJsonPath("$.reservationId", equalTo(response.getBody().id().toString()))));
        assertThat(pmsHttp.findAllUnmatchedRequests()).isEmpty();
    }

    @Test
    void simultaneousLastRoomHasExactlyOneWinner() throws Exception {
        jdbc.update("UPDATE room_type SET rooms=1 WHERE id=?", ROOM);
        var gate = new CountDownLatch(1);
        var ready = new CountDownLatch(2);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<Integer> task = () -> {
                ready.countDown();
                assertThat(gate.await(10, TimeUnit.SECONDS)).isTrue();
                return book(UUID.randomUUID().toString(), request(UUID.randomUUID()+"@example.com")).getStatusCode().value();
            }
            ;
            var first = pool.submit(task);
            var second = pool.submit(task);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            gate.countDown();
            assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS))).containsExactlyInAnyOrder(201,
                409);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reservation", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT sold FROM room_inventory", Integer.class)).containsExactly(1,
            1);
    }

    @Test
    void duplicateKeyReturnsSameReservationAndDifferentPayloadConflicts() {
        var req = request("deniz@example.com");
        var first = book("repeat-key-123", req);
        var repeat = book("repeat-key-123", req);
        assertThat(first.getStatusCode().value()).isEqualTo(201);
        assertThat(repeat.getBody()).isEqualTo(first.getBody());
        assertThat(book("repeat-key-123", request("other@example.com")).getStatusCode().value()).isEqualTo(409);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reservation", Long.class)).isEqualTo(1);
    }

    @Test
    void simultaneousIdenticalKeyReturnsOneReceipt() throws Exception {
        var req = request("same@example.com");
        var gate = new CountDownLatch(1);
        var ready = new CountDownLatch(2);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<ReservationReceipt> task = () -> {
                ready.countDown();
                gate.await(10, TimeUnit.SECONDS);
                return book("same-concurrent-key", req).getBody();
            }
            ;
            var a = pool.submit(task);
            var b = pool.submit(task);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            gate.countDown();
            assertThat(a.get(20, TimeUnit.SECONDS)).isEqualTo(b.get(20, TimeUnit.SECONDS));
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reservation", Long.class)).isEqualTo(1);
    }

    @Test
    void cancellationRestoresInventoryExactlyOnceAndRejectsForeignTenant() {
        jdbc.update("UPDATE room_type SET rooms=1 WHERE id=?", ROOM);
        var receipt = book("cancel-booking", request("cancel@example.com")).getBody();
        String path = "/api/crm/reservations/"+receipt.id()+"/cancel";
        assertThat(http.withBasicAuth("peri", "DemoHotel2026!").postForEntity(path, null, String.class).getStatusCode().value()).isEqualTo(404);
        assertThat(http.withBasicAuth("lagos", "DemoHotel2026!").postForEntity(path, null, String.class).getStatusCode().value()).isEqualTo(200);
        assertThat(http.withBasicAuth("lagos", "DemoHotel2026!").postForEntity(path, null, String.class).getStatusCode().value()).isEqualTo(200);
        assertThat(jdbc.queryForList("SELECT sold FROM room_inventory", Integer.class)).containsExactly(0,
            0);
        assertThat(book("booking-after-cancel", request("new@example.com")).getStatusCode().value()).isEqualTo(201);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_event WHERE event_type='ReservationCancelled'",
            Long.class)).isEqualTo(1);
    }

    @Test
    void asynchronousCrmDeduplicatesAndKeepsCancellationOrder() {
        var first = book("guest-first-book", request("deniz@example.com")).getBody();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM guest", Long.class)).isZero();
        service.handle(new ChangeReservationStatusHandler.Input(HOTEL, first.id(), com.hotelbooking.reservation.domain.ReservationStatus.CANCELLED));
        book("guest-return-book", request("DENIZ@example.com"));
        var worker = worker();
        worker.run();
        worker.run();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM guest", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM crm_reservation WHERE reservation_id=?",
            String.class, first.id())).isEqualTo("CANCELLED");
        jdbc.update("UPDATE event_delivery SET processed_at=NULL WHERE consumer='CRM'");
        worker.run();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM crm_reservation", Long.class)).isEqualTo(2);
        var rows = http.withBasicAuth("peri", "DemoHotel2026!").getForEntity("/api/crm/guests", String.class);
        assertThat(rows.getBody()).contains("\"total\":0");
        UUID guest = jdbc.queryForObject("SELECT id FROM guest", UUID.class);
        assertThat(http.withBasicAuth("peri", "DemoHotel2026!").getForEntity("/api/crm/guests/"+guest,
            String.class).getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void multipleNightFailureRollsBackEarlierNightAndOutbox() {
        jdbc.update("UPDATE room_type SET rooms=1 WHERE id=?", ROOM);
        jdbc.update("INSERT INTO room_inventory VALUES (?,?,?,?,?)", HOTEL, ROOM, in.plusDays(1), 1, 1);
        assertThat(book("rollback-booking", request("rollback@example.com")).getStatusCode().value()).isEqualTo(409);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM room_inventory WHERE stay_date=?", Long.class,
            in)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reservation", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_event", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM idempotency_record", Long.class)).isZero();
    }

    @Test
    void validatesConsentCapacityDatesAuthenticationAndCors() {
        assertThat(http.getForEntity("/api/crm/guests", String.class).getStatusCode().value()).isEqualTo(401);
        var invalid = new BookingRequest(ROOM, in, in.plusDays(1), 2, 0, "D", "T", "test@example.com",
            "+905551234567", false, false, null);
        assertThat(book("no-consent-booking", invalid).getStatusCode().value()).isEqualTo(400);
        assertThat(http.getForEntity("/api/public/hotels/lagos-beach/availability?checkIn="+in+"&checkOut="+in+"&adults=2",
            String.class).getStatusCode().value()).isEqualTo(400);
        var headers = new HttpHeaders();
        headers.setOrigin("https://attacker.example");
        assertThat(http.exchange("/api/public/hotels/lagos-beach", HttpMethod.GET, new HttpEntity<>(headers),
            String.class).getStatusCode().value()).isEqualTo(403);
        headers.setOrigin("http://localhost:8081");
        assertThat(http.exchange("/api/public/hotels/lagos-beach", HttpMethod.GET, new HttpEntity<>(headers),
            String.class).getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:8081");
        var other = new BookingRequest(UUID.fromString("20000000-0000-0000-0000-000000000005"), in,
            in.plusDays(1), 2, 0, "D", "T", "test@example.com", "+905551234567", true, false, null);
        assertThat(book("wrong-hotel-room", other).getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void cacheChangesAfterCommitAndDepartureDoesNotOverlap() {
        jdbc.update("UPDATE room_type SET rooms=1 WHERE id=?", ROOM);
        String path = "/api/public/hotels/lagos-beach/availability?checkIn="+in+"&checkOut="+in.plusDays(2)+"&adults=2";
        assertThat(http.getForEntity(path, String.class).getBody()).contains(ROOM.toString());
        var receipt = book("cache-booking-key", request("cache@example.com")).getBody();
        assertThat(http.getForEntity(path, String.class).getBody()).doesNotContain(ROOM.toString());
        String after = "/api/public/hotels/lagos-beach/availability?checkIn="+in.plusDays(2)+"&checkOut="+in.plusDays(3)+"&adults=2";
        assertThat(http.getForEntity(after, String.class).getBody()).contains(ROOM.toString());
        service.handle(new ChangeReservationStatusHandler.Input(HOTEL, receipt.id(), com.hotelbooking.reservation.domain.ReservationStatus.CANCELLED));
        assertThat(http.getForEntity(path, String.class).getBody()).contains(ROOM.toString());
    }

    @Test
    void emailOrPhoneReusesAProfileAndRegistersContactAliases() throws Exception {
        var first = request("first@example.com");
        book("first-guest-key", first);
        var second = new BookingRequest(ROOM, in, in.plusDays(2), 2, 0, "Second", "Guest", first.email(),
            "+905559999999", true, false, null);
        book("second-guest-key", second);
        var conflict = new BookingRequest(ROOM, in, in.plusDays(2), 2, 0, "Returning", "Guest", "different@example.com",
            second.phone(), true, false, null);
        book("conflict-guest-key", conflict);
        book("repeat-contact-pair", second);
        worker().run();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM guest", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM guest WHERE matching_conflict",
            Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM guest_identifier", Long.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM crm_reservation", Long.class)).isEqualTo(4);
        var aliasSearch = http.withBasicAuth("lagos", "DemoHotel2026!")
            .getForEntity("/api/crm/guests?email=different@example.com&phone=9999999", String.class);
        assertThat(json.read(aliasSearch.getBody(), com.fasterxml.jackson.databind.JsonNode.class).get("total").asInt()).isEqualTo(1);
    }

    @Test
    void separateGuestFiltersRequireOneMatchingReservationAndStayTenantScoped() throws Exception {
        var day = LocalDate.of(2027, 3, 1);
        projectGuest("Deniz", "Yılmaz", "deniz@example.com", "+905550000001", day, day.plusDays(2));
        projectGuest("Deniz", "Yılmaz", "deniz@example.com", "+905550000001", day.plusDays(10), day.plusDays(12));
        projectGuest("Deniz", "Kaya", "other@example.com", "+905550000002", day, day.plusDays(2));
        var lagos = http.withBasicAuth("lagos", "DemoHotel2026!");
        var response = lagos.getForEntity("/api/crm/guests?firstName=Deniz&lastName=Yılmaz&email=deniz&phone=000001"
            + "&checkInFrom=2027-03-01&checkInTo=2027-03-01&checkOutFrom=2027-03-03&checkOutTo=2027-03-03", String.class);
        assertThat(json.read(response.getBody(), com.fasterxml.jackson.databind.JsonNode.class).get("total").asInt()).isEqualTo(1);
        var crossReservation = lagos.getForEntity("/api/crm/guests?email=deniz&checkInTo=2027-03-01&checkOutFrom=2027-03-13",
            String.class);
        assertThat(json.read(crossReservation.getBody(), com.fasterxml.jackson.databind.JsonNode.class).get("total").asInt()).isZero();
        assertThat(lagos.getForEntity("/api/crm/guests?checkInFrom=2027-03-03&checkInTo=2027-03-01", String.class)
            .getStatusCode().value()).isEqualTo(400);
        assertThat(json.read(http.withBasicAuth("peri", "DemoHotel2026!")
            .getForEntity("/api/crm/guests?firstName=Deniz", String.class).getBody(), com.fasterxml.jackson.databind.JsonNode.class).get("total").asInt()).isZero();
    }

    @Test
    void sharedContactsAutomaticallyConsolidateProfilesHistoryNotesAndAuditWithoutStaffMatching() {
        var day = LocalDate.of(2027, 3, 1);
        var first = projectGuest("Ali", "Yılmaz", "first@example.com", "+905550000001", day, day.plusDays(2));
        var second = projectGuest("Veli", "Kaya", "second@example.com", "+905550000002", day, day.plusDays(2));
        var ids = jdbc.queryForList("SELECT id FROM guest ORDER BY id", UUID.class);
        UUID target = ids.getFirst(), source = ids.getLast();
        var lagos = http.withBasicAuth("lagos", "DemoHotel2026!");
        UUID note = lagos.postForObject("/api/crm/guests/" + source + "/notes", Map.of("body", "Kontrol edildi"),
            com.hotelbooking.guest.application.model.CreatedId.class).id();
        jdbc.update("UPDATE guest_note SET created_at=? WHERE id=?", java.sql.Timestamp.from(java.time.Instant.now().minusSeconds(90000)), note);
        lagos.postForEntity("/api/crm/guests/" + source + "/tags", Map.of("tag", "VIP"), String.class);
        lagos.postForEntity("/api/crm/guests/" + target + "/tags", Map.of("tag", "VIP"), String.class);
        var bridge = projectGuest("Different", "Name", first.email(), second.phone(), day, day.plusDays(2));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM guest WHERE merged_into IS NULL", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT guest_id FROM crm_reservation", UUID.class)).containsOnly(target).hasSize(3);
        assertThat(jdbc.queryForObject("SELECT guest_id FROM guest_note WHERE id=?", UUID.class, note)).isEqualTo(target);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM guest_tag WHERE guest_id=?", Integer.class, target)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT merged_into FROM guest WHERE id=?", UUID.class, source)).isEqualTo(target);
        assertThat(jdbc.queryForObject("SELECT resolved_by FROM guest_match_audit WHERE source_guest_id=?", String.class, source))
            .isEqualTo("SYSTEM_CONTACT_MATCH");
        var before = json.read(jdbc.queryForObject("SELECT before_state::text FROM guest_match_audit WHERE source_guest_id=?", String.class, source),
            com.fasterxml.jackson.databind.JsonNode.class);
        var after = json.read(jdbc.queryForObject("SELECT after_state::text FROM guest_match_audit WHERE source_guest_id=?", String.class, source),
            com.fasterxml.jackson.databind.JsonNode.class);
        assertThat(before.at("/source/notes/0/body").asText()).isEqualTo("Kontrol edildi");
        assertThat(after.at("/target/notes/0/body").asText()).isEqualTo("Kontrol edildi");
        assertThat(lagos.exchange("/api/crm/guests/" + target + "/notes/" + note, HttpMethod.DELETE, null, String.class)
            .getStatusCode().value()).isEqualTo(409);
        assertThat(lagos.getForEntity("/api/crm/guests/" + target + "/matching", String.class).getStatusCode().value()).isEqualTo(404);
        assertThat(lagos.postForEntity("/api/crm/guests/" + target + "/matching", Map.of("targetGuestId", source), String.class)
            .getStatusCode().value()).isEqualTo(404);
        projectGuest("No", "NameCheck", second.email(), "+905550000003", day, day.plusDays(2));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM guest WHERE merged_into IS NULL", Integer.class)).isEqualTo(1);
        var detail = json.read(lagos.getForObject("/api/crm/guests/" + target, String.class), com.fasterxml.jackson.databind.JsonNode.class);
        assertThat(detail.path("emails").toString()).contains(first.email(), second.email());
        assertThat(detail.path("phones").toString()).contains(first.phone(), second.phone(), "+905550000003");
        assertThat(jdbc.queryForObject("SELECT guest_id FROM crm_reservation WHERE reservation_id=?", UUID.class, bridge.id())).isEqualTo(target);
        projectGuest("Ali", "Yılmaz", "unrelated@example.com", "+905550000099", day, day.plusDays(2));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM guest WHERE merged_into IS NULL", Integer.class)).isEqualTo(2);
    }

    @Test
    void auditFailureRollsBackAutomaticConsolidationAndProjectionTogether() {
        var day = LocalDate.of(2027, 3, 1);
        projectGuest("Ali", "Yılmaz", "first@example.com", "+905550000001", day, day.plusDays(2));
        projectGuest("Veli", "Kaya", "second@example.com", "+905550000002", day, day.plusDays(2));
        var ids = jdbc.queryForList("SELECT id FROM guest ORDER BY id", UUID.class);
        UUID source = ids.getLast();
        http.withBasicAuth("lagos", "DemoHotel2026!").postForEntity("/api/crm/guests/" + source + "/notes",
            Map.of("body", "Keep original"), String.class);
        jdbc.execute("ALTER TABLE guest_match_audit ADD CONSTRAINT reject_test_audit CHECK (false) NOT VALID");
        try {
            assertThatThrownBy(() -> projectGuest("Bridge", "Guest", "first@example.com", "+905550000002", day, day.plusDays(2)))
                .isInstanceOf(RuntimeException.class);
        } finally {
            jdbc.execute("ALTER TABLE guest_match_audit DROP CONSTRAINT reject_test_audit");
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM guest_match_audit", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM guest WHERE merged_into IS NULL", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM crm_reservation", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM guest_note WHERE guest_id=?", Integer.class, source)).isEqualTo(1);
    }

    private com.hotelbooking.contracts.reservation.ReservationSnapshot projectGuest(String firstName, String lastName,
        String email, String phone, LocalDate checkIn, LocalDate checkOut) {
        var snapshot = new com.hotelbooking.contracts.reservation.ReservationSnapshot(UUID.randomUUID(), HOTEL,
            "RT-" + UUID.randomUUID().toString().substring(0, 12), checkIn, checkOut, firstName, lastName,
            email, phone, false, java.time.Instant.now(), "CONFIRMED", java.math.BigDecimal.TEN, 1, null);
        projection.handle(new ApplyReservationProjectionHandler.Input(snapshot));
        return snapshot;
    }

    @Test
    void savedSegmentsNotesAndTagsRemainTenantScoped() {
        book("profile-management", request("profile@example.com"));
        worker().run();
        UUID guest = jdbc.queryForObject("SELECT id FROM guest", UUID.class);
        var lagos = http.withBasicAuth("lagos", "DemoHotel2026!");
        var peri = http.withBasicAuth("peri", "DemoHotel2026!");
        String profile = "/api/crm/guests/"+guest;
        assertThat(lagos.postForEntity(profile+"/notes", Map.of("body", "Sessiz oda"), String.class).getStatusCode().value()).isEqualTo(200);
        assertThat(lagos.postForEntity(profile+"/tags", Map.of("tag", "VIP"), String.class).getStatusCode().value()).isEqualTo(200);
        assertThat(peri.postForEntity(profile+"/notes", Map.of("body", "Forbidden"), String.class).getStatusCode().value()).isEqualTo(404);
        assertThat(lagos.getForEntity(profile, String.class).getBody()).contains("Sessiz oda", "VIP");
        var response = lagos.postForEntity("/api/crm/segments", Map.of("name", "Tüm profiller", "minStays",
            0, "months", 12, "marketingRequired", false), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(lagos.postForEntity("/api/crm/segments", Map.of("name", "Obsolete day filter", "minStays", 0,
            "months", 12, "daysSinceCheckout", 30, "marketingRequired", false), String.class).getStatusCode().value()).isEqualTo(400);
        UUID segment = jdbc.queryForObject("SELECT id FROM guest_segment", UUID.class);
        var segmentGuests = lagos.getForEntity("/api/crm/guests?segment="+segment, String.class);
        assertThat(segmentGuests.getStatusCode().value()).isEqualTo(200);
        assertThat(segmentGuests.getBody()).contains("profile@example.com");
        for (String builtIn : List.of("RETURNING", "REENGAGE")) {
            var filtered = lagos.getForEntity("/api/crm/guests?segment="+builtIn, String.class);
            assertThat(filtered.getStatusCode().value()).isEqualTo(200);
            assertThat(filtered.getBody()).doesNotContain("profile@example.com");
        }
        assertThat(peri.getForEntity("/api/crm/guests?segment="+segment, String.class).getStatusCode().value()).isEqualTo(404);
        assertThat(peri.exchange("/api/crm/segments/"+segment, HttpMethod.DELETE, null, String.class).getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void guestNotesCanBeDeletedOnlyByTheirAuthorBeforeTwentyFourHoursAndWithinTheirTenant() {
        var snapshot = projectGuest("Note", "Guest", "notes@example.com", "+905550001122", in, in.plusDays(2));
        UUID guest = jdbc.queryForObject("SELECT guest_id FROM crm_reservation WHERE reservation_id=?", UUID.class, snapshot.id());
        var lagos = http.withBasicAuth("lagos", "DemoHotel2026!");
        String notes = "/api/crm/guests/" + guest + "/notes";
        UUID recent = lagos.postForObject(notes, Map.of("body", "Wrong note"), com.hotelbooking.guest.application.model.CreatedId.class).id();
        String recentPath = notes + "/" + recent;
        assertThat(http.exchange(recentPath, HttpMethod.DELETE, null, String.class).getStatusCode().value()).isEqualTo(401);
        assertThat(http.withBasicAuth("peri", "DemoHotel2026!").exchange(recentPath, HttpMethod.DELETE, null, String.class)
            .getStatusCode().value()).isEqualTo(404);
        assertThat(lagos.exchange("/api/crm/guests/" + UUID.randomUUID() + "/notes/" + recent,
            HttpMethod.DELETE, null, String.class).getStatusCode().value()).isEqualTo(404);
        assertThat(lagos.exchange(recentPath, HttpMethod.DELETE, null, String.class).getStatusCode().value()).isEqualTo(204);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM guest_note WHERE id=?", Integer.class, recent)).isZero();
        assertThat(lagos.exchange(recentPath, HttpMethod.DELETE, null, String.class).getStatusCode().value()).isEqualTo(404);

        UUID expired = lagos.postForObject(notes, Map.of("body", "Historical note"), com.hotelbooking.guest.application.model.CreatedId.class).id();
        jdbc.update("UPDATE guest_note SET created_at=? WHERE id=?", java.sql.Timestamp.from(java.time.Instant.now().minusSeconds(86405)), expired);
        var blocked = lagos.exchange(notes + "/" + expired, HttpMethod.DELETE, null, String.class);
        assertThat(blocked.getStatusCode().value()).isEqualTo(409);
        assertThat(blocked.getBody()).contains("NOTE_DELETE_WINDOW_CLOSED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM guest_note WHERE id=?", Integer.class, expired)).isEqualTo(1);

        UUID other = lagos.postForObject(notes, Map.of("body", "Another staff's note"), com.hotelbooking.guest.application.model.CreatedId.class).id();
        jdbc.update("UPDATE guest_note SET author='other-staff' WHERE id=?", other);
        assertThat(lagos.exchange(notes + "/" + other, HttpMethod.DELETE, null, String.class).getStatusCode().value()).isEqualTo(403);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM guest_note WHERE id=?", Integer.class, other)).isEqualTo(1);
    }

    @Test
    void invalidStateAndPrematureCheckInAreRejected() {
        var receipt = book("state-machine-book", request("status@example.com")).getBody();
        var staff = http.withBasicAuth("lagos", "DemoHotel2026!");
        String path = "/api/crm/reservations/"+receipt.id()+"/status";
        assertThat(staff.postForEntity(path, Map.of("status", "CHECKED_OUT"), String.class).getStatusCode().value()).isEqualTo(409);
        assertThat(staff.postForEntity(path, Map.of("status", "CHECKED_IN"), String.class).getStatusCode().value()).isEqualTo(400);
        assertThat(staff.postForEntity(path, Map.of("status", "CANCELLED"), String.class).getStatusCode().value()).isEqualTo(200);
        assertThat(staff.postForEntity(path, Map.of("status", "CONFIRMED"), String.class).getStatusCode().value()).isEqualTo(409);
    }
    @Test
    void bookingDetailsPersistServicePricesOccupancyAndProfileAndStayTenantScoped() {
        var base = request("services@example.com");
        var body = new BookingRequest(base.roomTypeId(), base.checkIn(), base.checkOut(), base.adults(), base.children(),
            base.firstName(), base.lastName(), base.email(), base.phone(), true, true, base.sessionId(), List.of("LUNCH",
            "VALET", "TRANSFER"));
        var receipt = book("booking-with-extras", body).getBody();
        worker().run();
        var staff = http.withBasicAuth("lagos", "DemoHotel2026!");
        String path = "/api/crm/reservations/"+receipt.id();
        var response = staff.getForEntity(path, String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        var detail = json.read(response.getBody(), com.fasterxml.jackson.databind.JsonNode.class);
        assertThat(detail.path("adults").asInt()).isEqualTo(2);
        assertThat(detail.path("children").asInt()).isZero();
        assertThat(detail.path("room_name").asText()).isEqualTo("Standart");
        assertThat(detail.path("guest_id").asText()).isNotBlank();
        assertThat(detail.path("services").size()).isEqualTo(5);
        // 2 guests x 2 nights x 650 + 2 nights x 300 + one transfer 1800.
        assertThat(receipt.total().subtract(detail.path("room_total").decimalValue())).isEqualByComparingTo("5000");
        var guest = staff.getForEntity("/api/crm/guests/"+detail.path("guest_id").asText(), String.class);
        assertThat(guest.getBody()).contains(receipt.id().toString());
        assertThat(http.withBasicAuth("peri", "DemoHotel2026!").getForEntity(path, String.class).getStatusCode().value()).isEqualTo(404);
        assertThat(http.getForEntity(path, String.class).getStatusCode().value()).isEqualTo(401);
        jdbc.update("UPDATE hotel_service SET price=9999 WHERE hotel_id=? AND code='LUNCH'", HOTEL);
        try {
            assertThat(staff.getForEntity(path, String.class).getBody()).isEqualTo(response.getBody());
            assertThat(book("booking-with-extras", body).getBody()).isEqualTo(receipt);
        } finally {
            jdbc.update("UPDATE hotel_service SET price=650 WHERE hotel_id=? AND code='LUNCH'", HOTEL);
        }
    }

    @Test
    void unknownAndDuplicateServicesRollbackAndSelectionChangesCannotReuseAnIdempotencyKey() {
        var base = request("invalid-services@example.com");
        for (var codes : List.of(List.of("UNKNOWN"), List.of("VALET", "VALET"))) {
            var invalid = new BookingRequest(base.roomTypeId(), base.checkIn(), base.checkOut(), 2, 0,
                base.firstName(), base.lastName(), base.email(), base.phone(), true, false, null, codes);
            assertThat(book("invalid-service-selection", invalid).getStatusCode().value()).isEqualTo(400);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM reservation", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM idempotency_record", Integer.class)).isZero();
        }
        assertThat(book("selection-idempotency", base).getStatusCode().value()).isEqualTo(201);
        var changed = new BookingRequest(base.roomTypeId(), base.checkIn(), base.checkOut(), 2, 0,
            base.firstName(), base.lastName(), base.email(), base.phone(), true, true, base.sessionId(), List.of("VALET"));
        assertThat(book("selection-idempotency", changed).getStatusCode().value()).isEqualTo(409);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reservation", Integer.class)).isEqualTo(1);
    }

    @Test
    void reservationFiltersCombineBeforePaginationAndValidateInvalidRanges() {
        var receipt = book("filter-booking-one", request("filter@example.com")).getBody();
        var staff = http.withBasicAuth("lagos", "DemoHotel2026!");
        String filter = "/api/crm/reservations?firstName=Deniz&lastName=Test&email=filter&phone=555&status=CONFIRMED"
        +"&roomTypeId="+ROOM+"&adults=2&children=0&checkInFrom="+in+"&checkInTo="+in
        +"&checkOutFrom="+in.plusDays(2)+"&checkOutTo="+in.plusDays(2)+"&minTotal=1&maxTotal=999999&size=1";
        var found = json.read(staff.getForEntity(filter, String.class).getBody(), com.fasterxml.jackson.databind.JsonNode.class);
        assertThat(found.path("total").asInt()).isEqualTo(1);
        assertThat(found.at("/items/0/id").asText()).isEqualTo(receipt.id().toString());
        var next = json.read(staff.getForEntity(filter+"&page=1", String.class).getBody(), com.fasterxml.jackson.databind.JsonNode.class);
        assertThat(next.path("items").size()).isZero();
        assertThat(next.path("total").asInt()).isEqualTo(1);
        var other = json.read(http.withBasicAuth("peri", "DemoHotel2026!").getForEntity(filter, String.class).getBody(),
            com.fasterxml.jackson.databind.JsonNode.class);
        assertThat(other.path("total").asInt()).isZero();
        var absent = json.read(staff.getForEntity(filter+"&code=MISSING", String.class).getBody(), com.fasterxml.jackson.databind.JsonNode.class);
        assertThat(absent.path("total").asInt()).isZero();
        assertThat(staff.getForEntity("/api/crm/reservations?minTotal=50&maxTotal=1", String.class).getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void staffSortingAppliesToTheWholeFilteredResultBeforePagination() {
        var day = LocalDate.of(2027, 3, 1);
        projectGuest("Zeynep", "Sort", "z@example.com", "+905550000003", day, day.plusDays(2));
        projectGuest("Ada", "Sort", "a@example.com", "+905550000001", day, day.plusDays(2));
        projectGuest("Mert", "Sort", "m@example.com", "+905550000002", day, day.plusDays(2));
        var staff = http.withBasicAuth("lagos", "DemoHotel2026!");
        var first = json.read(staff.getForEntity("/api/crm/guests?sort=FIRST_NAME&direction=ASC&size=1&page=0",
            String.class).getBody(), com.fasterxml.jackson.databind.JsonNode.class);
        var second = json.read(staff.getForEntity("/api/crm/guests?sort=FIRST_NAME&direction=ASC&size=1&page=1",
            String.class).getBody(), com.fasterxml.jackson.databind.JsonNode.class);
        var reverse = json.read(staff.getForEntity("/api/crm/guests?sort=FIRST_NAME&direction=DESC&size=1&page=0",
            String.class).getBody(), com.fasterxml.jackson.databind.JsonNode.class);
        assertThat(first.at("/items/0/first_name").asText()).isEqualTo("Ada");
        assertThat(second.at("/items/0/first_name").asText()).isEqualTo("Mert");
        assertThat(reverse.at("/items/0/first_name").asText()).isEqualTo("Zeynep");
        assertThat(first.get("total").asInt()).isEqualTo(3);
        assertThat(staff.getForEntity("/api/crm/guests?sort=unknown", String.class).getStatusCode().value()).isEqualTo(400);
        assertThat(staff.getForEntity("/api/crm/reservations?direction=sideways", String.class).getStatusCode().value()).isEqualTo(400);
        var other = json.read(http.withBasicAuth("peri", "DemoHotel2026!").getForEntity("/api/crm/guests?sort=EMAIL&direction=DESC",
            String.class).getBody(), com.fasterxml.jackson.databind.JsonNode.class);
        assertThat(other.get("total").asInt()).isZero();
    }

    @Test
    void reservationSortUsesNumericTotalsAndCalendarDatesAcrossPages() {
        var staff = http.withBasicAuth("lagos", "DemoHotel2026!");
        var shorter = new BookingRequest(ROOM, in, in.plusDays(1), 2, 0, "Short", "Stay", "short@example.com",
            "+905550000011", true, false, null);
        var longer = new BookingRequest(ROOM, in.plusDays(4), in.plusDays(7), 2, 0, "Long", "Stay", "long@example.com",
            "+905550000012", true, false, null);
        var small = book("sorting-short", shorter).getBody();
        var large = book("sorting-long", longer).getBody();
        var byTotal = json.read(staff.getForEntity("/api/crm/reservations?sort=TOTAL&direction=DESC&size=1&page=0",
            String.class).getBody(), com.fasterxml.jackson.databind.JsonNode.class);
        var nextPage = json.read(staff.getForEntity("/api/crm/reservations?sort=TOTAL&direction=DESC&size=1&page=1",
            String.class).getBody(), com.fasterxml.jackson.databind.JsonNode.class);
        var byArrival = json.read(staff.getForEntity("/api/crm/reservations?sort=CHECK_IN&direction=ASC&size=1",
            String.class).getBody(), com.fasterxml.jackson.databind.JsonNode.class);
        assertThat(byTotal.at("/items/0/id").asText()).isEqualTo(large.id().toString());
        assertThat(nextPage.at("/items/0/id").asText()).isEqualTo(small.id().toString());
        assertThat(byArrival.at("/items/0/id").asText()).isEqualTo(small.id().toString());
    }

    @Test
    void publicRoomCatalogHasPricesPhotosAndDetailsWithoutLeakingAnotherHotel() {
        var lagos = json.read(http.getForEntity("/api/public/hotels/lagos-beach/rooms", String.class).getBody(),
            com.fasterxml.jackson.databind.JsonNode.class);
        var peri = json.read(http.getForEntity("/api/public/hotels/peri-stone/rooms", String.class).getBody(),
            com.fasterxml.jackson.databind.JsonNode.class);
        assertThat(lagos.size()).isEqualTo(3);
        assertThat(peri.size()).isEqualTo(2);
        assertThat(lagos.get(0).get("nightlyPrice").decimalValue()).isEqualByComparingTo("3200.00");
        assertThat(lagos.get(0).get("areaSquareMeters").asInt()).isEqualTo(28);
        assertThat(lagos.get(0).get("capacity").asInt()).isEqualTo(2);
        assertThat(lagos.get(0).get("currency").asText()).isEqualTo("TRY");
        assertThat(lagos.get(0).get("images").get(0).asText()).isEqualTo("/images/showcase/lagos-room.webp");
        assertThat(lagos.get(0).get("amenities").size()).isGreaterThan(0);
        var galleryImages = new java.util.HashSet<String>();
        for (var catalog : java.util.List.of(lagos, peri)) {
            for (var room : catalog) {
                assertThat(room.get("images").size()).isEqualTo(5);
                room.get("images").forEach(image -> galleryImages.add(image.asText()));
            }
        }
        assertThat(galleryImages).hasSize(25);
        assertThat(lagos.toString()).doesNotContain("Mağara Oda", "Teras Süit");
        assertThat(peri.get(0).get("nightlyPrice").decimalValue()).isEqualByComparingTo("5500.00");
        assertThat(peri.toString()).doesNotContain(ROOM.toString());
        assertThat(http.getForEntity("/api/public/hotels/unknown/rooms", String.class).getStatusCode().value()).isEqualTo(404);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM room_inventory", Long.class)).isZero();
    }

    @Test
    void swaggerDocumentsPublicAndAuthenticatedEndpoints() throws Exception {
        var ui = http.getForEntity("/swagger-ui/index.html", String.class);
        assertThat(ui.getStatusCode().value()).isEqualTo(200);
        assertThat(ui.getBody()).contains("Swagger UI");
        var response = http.getForEntity("/v3/api-docs", String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        var document = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.getBody());
        assertThat(document.path("components").path("securitySchemes").path("basicAuth").path("scheme").asText())
            .isEqualTo("basic");
        var paths = document.path("paths");
        assertThat(paths.has("/api/public/hotels/{key}/availability")).isTrue();
        assertThat(paths.has("/api/crm/guests")).isTrue();
        assertThat(paths.path("/api/crm/guests").path("get").path("security").get(0).has("basicAuth"))
            .isTrue();
        assertThat(paths.path("/api/public/hotels/{key}/availability").path("get").has("security"))
            .isFalse();
        assertThat(http.getForEntity("/api/crm/guests", String.class).getStatusCode().value()).isEqualTo(401);
    }
    @Test
    void happyPathArrivalCheckoutAndCrmCompletedStay() throws Exception {
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Istanbul"));
        var arriving = new BookingRequest(ROOM, today, today.plusDays(2), 2, 0, "Deniz", "Arriving",
            "arriving@example.com", "+905551234567", true, true, UUID.randomUUID());
        var receipt = book("arrival-happy-path", arriving).getBody();
        assertThat(receipt).isNotNull();
        var staff = http.withBasicAuth("lagos", "DemoHotel2026!");
        String path = "/api/crm/reservations/" + receipt.id() + "/status";
        assertThat(staff.postForEntity(path, Map.of("status", "CHECKED_IN"), String.class).getStatusCode().value())
            .isEqualTo(200);
        assertThat(staff.postForEntity(path, Map.of("status", "CHECKED_OUT"), String.class).getStatusCode().value())
            .isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT checked_out_at IS NOT NULL FROM reservation WHERE id=?",
            Boolean.class, receipt.id())).isTrue();
        worker().run();
        assertThat(staff.getForEntity("/api/crm/guests?q=arriving@example.com", String.class).getBody())
            .contains("\"stays\":1");
    }

    @Test
    void rejectsCapacityMinimumNightsAndUnknownFieldsWithoutWritingReservations() {
        var tooManyGuests = new BookingRequest(ROOM, in, in.plusDays(2), 3, 0, "D", "T",
            "capacity@example.com", "+905551234567", true, false, null);
        assertThat(book("capacity-edge-key", tooManyGuests).getStatusCode().value()).isEqualTo(400);
        jdbc.update("UPDATE room_type SET min_nights=3 WHERE id=?", ROOM);
        assertThat(book("minimum-edge-key", request("minimum@example.com")).getStatusCode().value()).isEqualTo(400);
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", "unknown-field-key");
        jdbc.update("UPDATE room_type SET min_nights=1 WHERE id=?", ROOM);
        String original = json.write(request("unknown-field@example.com"));
        String payload = original.substring(0, original.length() - 1) + ",\"unexpected\":true}";
        assertThat(http.postForEntity("/api/public/hotels/lagos-beach/reservations",
            new HttpEntity<>(payload, headers), String.class).getStatusCode().value()).isEqualTo(400);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reservation", Long.class)).isZero();
    }

    @Test
    void noShowOnArrivalReleasesInventoryOnlyOnce() {
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Istanbul"));
        var arriving = new BookingRequest(ROOM, today, today.plusDays(1), 2, 0, "Deniz", "NoShow",
            "noshow@example.com", "+905551234567", true, false, null);
        var receipt = book("noshow-happy-key", arriving).getBody();
        var staff = http.withBasicAuth("lagos", "DemoHotel2026!");
        String path = "/api/crm/reservations/" + receipt.id() + "/status";
        for (int i = 0; i < 2; i++) {
            assertThat(staff.postForEntity(path, Map.of("status", "NO_SHOW"), String.class).getStatusCode().value())
                .isEqualTo(200);
        }
        assertThat(jdbc.queryForList("SELECT sold FROM room_inventory", Integer.class)).containsExactly(0);
        assertThat(staff.postForEntity(path, Map.of("status", "CANCELLED"), String.class).getStatusCode().value())
            .isEqualTo(409);
    }
    @Test
    void staffIdentityReturnsOnlyTheAuthenticatedHotelsMetadata() throws Exception {
        var lagos = http.withBasicAuth("lagos", "DemoHotel2026!")
            .getForEntity("/api/auth/me", String.class);
        var peri = http.withBasicAuth("peri", "DemoHotel2026!")
            .getForEntity("/api/auth/me", String.class);
        assertThat(lagos.getStatusCode().value()).isEqualTo(200);
        assertThat(peri.getStatusCode().value()).isEqualTo(200);
        var lagosIdentity = new com.fasterxml.jackson.databind.ObjectMapper().readTree(lagos.getBody());
        var periIdentity = new com.fasterxml.jackson.databind.ObjectMapper().readTree(peri.getBody());
        assertThat(lagosIdentity.path("hotelName").asText()).isEqualTo("Lagos Beach Hotel");
        assertThat(lagosIdentity.path("hotelKey").asText()).isEqualTo("lagos-beach");
        assertThat(periIdentity.path("hotelName").asText()).isEqualTo("Peri Stone Cave Hotel");
        assertThat(periIdentity.path("hotelKey").asText()).isEqualTo("peri-stone");
        assertThat(lagos.getHeaders().getFirst("X-CSRF-TOKEN")).isNotBlank();
    }

    @Test
    void simultaneousCancellationAcknowledgesBothRequestsAndReleasesInventoryOnlyOnce() throws Exception {
        var receipt = book("parallel-cancel-key", request("cancel-race@example.com")).getBody();
        var gate = new CountDownLatch(1);
        var ready = new CountDownLatch(2);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<Integer> cancel = () -> {
                ready.countDown();
                assertThat(gate.await(10, TimeUnit.SECONDS)).isTrue();
                return http.withBasicAuth("lagos", "DemoHotel2026!")
                    .postForEntity("/api/crm/reservations/" + receipt.id() + "/status",
                    Map.of("status", "CANCELLED"), String.class).getStatusCode().value();
            };
            var first = pool.submit(cancel);
            var second = pool.submit(cancel);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            gate.countDown();
            assertThat(first.get(20, TimeUnit.SECONDS)).isEqualTo(200);
            assertThat(second.get(20, TimeUnit.SECONDS)).isEqualTo(200);
        }
        assertThat(jdbc.queryForObject("SELECT sum(sold) FROM room_inventory", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT version FROM reservation WHERE id=?", Integer.class, receipt.id())).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_event WHERE event_type='ReservationCancelled'",
            Integer.class)).isEqualTo(1);
    }

    @Test
    void dailyPmsHappyPathHonorsStrictJsonTenantBoundariesAndRoomLifecycle() throws Exception {
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Istanbul"));
        var body = new BookingRequest(ROOM, today, today.plusDays(2), 2, 0, "Daily", "Guest", "daily-flow@example.com",
            "+905551234567", true, false, null, List.of("LUNCH", "VALET", "TRANSFER"));
        var receipt = book("daily-pms-happy-path", body).getBody();
        assertThat(receipt).isNotNull();
        UUID physical = jdbc.queryForObject("SELECT id FROM pms_room WHERE hotel_id=? AND room_type_id=? ORDER BY number LIMIT 1",
            UUID.class, HOTEL, ROOM);
        var staff = http.withBasicAuth("lagos", "DemoHotel2026!");
        String booking = "/api/crm/pms/reservations/"+receipt.id();
        String status = "/api/crm/reservations/"+receipt.id()+"/status";
        assertThat(http.getForEntity("/api/crm/pms/daily", String.class).getStatusCode().value()).isEqualTo(401);
        assertThat(staff.postForEntity(booking+"/room", Map.of("roomId", physical), String.class).getStatusCode().value()).isEqualTo(204);
        String cleaning = "/api/crm/pms/rooms/"+physical+"/housekeeping";
        assertThat(staff.postForEntity(cleaning, Map.of("state", "DIRTY"), String.class).getStatusCode().value()).isEqualTo(204);
        assertThat(staff.postForEntity(status, Map.of("status", "CHECKED_IN"), String.class).getStatusCode().value()).isEqualTo(409);
        assertThat(jdbc.queryForObject("SELECT status FROM reservation WHERE id=?", String.class, receipt.id())).isEqualTo("CONFIRMED");
        assertThat(staff.postForEntity(cleaning, Map.of("state", "CLEAN"), String.class).getStatusCode().value()).isEqualTo(204);
        assertThat(staff.postForEntity(status, Map.of("status", "CHECKED_IN"), String.class).getStatusCode().value()).isEqualTo(200);
        var meal = Map.of("date", today.toString(), "status", "SERVED", "passengers", 0, "notes", "Lunch served");
        assertThat(staff.postForEntity(booking+"/daily-services/LUNCH", meal, String.class).getStatusCode().value()).isEqualTo(204);
        assertThat(staff.postForEntity(booking+"/daily-services/LUNCH", meal, String.class).getStatusCode().value()).isEqualTo(204);
        assertThat(staff.postForEntity(booking+"/daily-services/LUNCH", Map.of("date", today.toString(), "status",
            "SERVED", "actor", "forged"), String.class).getStatusCode().value()).isEqualTo(400);
        assertThat(staff.postForEntity(booking+"/daily-services/LUNCH", Map.of("date", today.plusDays(1).toString(),
            "status", "SERVED"), String.class).getStatusCode().value()).isEqualTo(400);
        assertThat(http.withBasicAuth("peri", "DemoHotel2026!").postForEntity(booking+"/daily-services/LUNCH",
            meal, String.class).getStatusCode().value()).isEqualTo(404);
        var todayPlan = json.read(staff.getForEntity("/api/crm/pms/daily?date="+today, String.class).getBody(),
            com.hotelbooking.pms.application.model.DailyOperations.class);
        assertThat(todayPlan.services()).filteredOn(item -> item.reservationId().equals(receipt.id()) && item.task().code().equals("LUNCH"))
            .hasSize(1).allMatch(item -> item.task().status() == com.hotelbooking.pms.domain.DailyServiceStatus.SERVED);
        var tomorrowPlan = json.read(staff.getForEntity("/api/crm/pms/daily?date="+today.plusDays(1), String.class).getBody(),
            com.hotelbooking.pms.application.model.DailyOperations.class);
        assertThat(tomorrowPlan.services()).filteredOn(item -> item.reservationId().equals(receipt.id()) && item.task().code().equals("LUNCH"))
            .hasSize(1).allMatch(item -> item.task().status() == com.hotelbooking.pms.domain.DailyServiceStatus.PENDING);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pms_daily_service_audit WHERE reservation_id=? AND code='LUNCH'",
            Long.class, receipt.id())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT actor FROM pms_daily_service_audit WHERE reservation_id=? AND code='LUNCH'",
            String.class, receipt.id())).isEqualTo("lagos");
        assertThat(staff.postForEntity(status, Map.of("status", "CHECKED_OUT"), String.class).getStatusCode().value()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT housekeeping FROM pms_room WHERE id=?", String.class, physical)).isEqualTo("DIRTY");
        assertThat(jdbc.queryForObject("SELECT total FROM reservation WHERE id=?", java.math.BigDecimal.class,
            receipt.id())).isEqualByComparingTo(receipt.total());
    }

    @Test
    void simultaneousPhysicalRoomAssignmentHasOneWinnerAndOneConflict() throws Exception {
        var first = book("physical-room-race-first", request("room-race-first@example.com")).getBody();
        var second = book("physical-room-race-second", request("room-race-second@example.com")).getBody();
        UUID room = jdbc.queryForObject("SELECT id FROM pms_room WHERE hotel_id=? AND room_type_id=? ORDER BY number LIMIT 1",
            UUID.class, HOTEL, ROOM);
        var ready = new CountDownLatch(2);
        var gate = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var jobs = java.util.stream.Stream.of(first, second).map(receipt -> pool.submit(() -> {
                ready.countDown();
                assertThat(gate.await(10, TimeUnit.SECONDS)).isTrue();
                return http.withBasicAuth("lagos", "DemoHotel2026!").postForEntity(
                    "/api/crm/pms/reservations/"+receipt.id()+"/room", Map.of("roomId", room), String.class).getStatusCode().value();
            })).toList();
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            gate.countDown();
            assertThat(List.of(jobs.get(0).get(20, TimeUnit.SECONDS), jobs.get(1).get(20, TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(204, 409);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pms_room_assignment WHERE room_id=?", Long.class,
            room)).isEqualTo(1);
    }

}
