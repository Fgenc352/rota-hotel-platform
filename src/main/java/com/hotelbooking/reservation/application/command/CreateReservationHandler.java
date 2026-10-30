package com.hotelbooking.reservation.application.command;

import com.hotelbooking.hotel.application.port.HotelCatalog;
import com.hotelbooking.inventory.application.port.InventoryCommands;
import com.hotelbooking.reservation.application.model.ReservationReceipt;
import com.hotelbooking.reservation.application.port.FunnelEvents;
import com.hotelbooking.reservation.application.port.IdempotencyRecords;
import com.hotelbooking.reservation.application.port.ReservationExtras;
import com.hotelbooking.reservation.application.port.Reservations;
import com.hotelbooking.reservation.application.port.ServiceCatalog;
import com.hotelbooking.reservation.domain.ServiceLine;
import com.hotelbooking.reservation.domain.ServicePricing;
import com.hotelbooking.reservation.event.ReservationEventPublisher;
import com.hotelbooking.shared.domain.Stay;
import com.hotelbooking.shared.exception.BusinessException;
import com.hotelbooking.shared.json.Json;
import com.hotelbooking.shared.logging.TransactionLog;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreateReservationHandler {
    public record Input(
        String hotelKey,
        String idempotencyKey,
        BookingData booking
        ) {
    }

    private final ServiceCatalog serviceCatalog;
    private final ReservationExtras extras;
    private final Reservations reservations;
    private final IdempotencyRecords idempotency;
    private final InventoryCommands inventory;
    private final HotelCatalog hotels;
    private final FunnelEvents funnel;
    private final Json json;
    private final ReservationEventPublisher events;

    @Transactional
    public ReservationReceipt handle(Input input) {
        String key = input.idempotencyKey();
        if (key == null || !key.matches("[a-zA-Z0-9_-]{8,100}")) {
            throw BusinessException.invalid("Idempotency-Key 8–100 karakter olmalıdır.");
        }
        var hotel = hotels.byKey(input.hotelKey());
        log.info("event=reservation.create_started hotelId={} roomTypeId={}", hotel.id(), input.booking().roomTypeId());
        var request = input.booking().normalized();
        if (!request.privacyAccepted()) {
            throw BusinessException.invalid("KVKK aydınlatma onayı gereklidir.");
        }
        String hash = sha256(json.write(request));
        var entry = idempotency.lock(hotel.id(), key, hash);
        if (!hash.equals(entry.hash())) {
            throw BusinessException.conflict("IDEMPOTENCY_CONFLICT", "Bu anahtar başka bir istek için kullanılmış.");
        }
        if (entry.response() != null) {
            var receipt = json.read(entry.response(), ReservationReceipt.class);
            log.info("event=reservation.replayed hotelId={} reservationId={}", hotel.id(), receipt.id());
            return receipt;
        }

        var stay = new Stay(request.checkIn(), request.checkOut(), request.adults(), request.children());
        stay.validateForBooking(LocalDate.now(ZoneId.of(hotel.timezone())));
        var room = inventory.room(hotel.id(), request.roomTypeId());
        room.validateStay(stay);
        inventory.reserve(room, stay);
        UUID id = UUID.randomUUID();
        String code = "RT-" + UUID.randomUUID().toString().replace("-",
            "").substring(0, 16).toUpperCase(Locale.ROOT);
        var roomTotal = room.total(stay);
        var serviceLines = ServicePricing.quote(serviceCatalog.services(hotel.id()), request.serviceCodes(),
            stay);
        var total = roomTotal.add(serviceLines.stream().map(ServiceLine::total)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
        reservations.insert(id, hotel.id(), code, request, total);
        reservations.insertNights(hotel.id(), id, room, stay);
        extras.save(hotel.id(), id, roomTotal, serviceLines);
        events.publish(hotel.id(), id, "ReservationCreated");
        if (request.sessionId() != null) {
            funnel.record(hotel.id(), request.sessionId(), "CONFIRMED");
        }
        var receipt = new ReservationReceipt(id, code, "CONFIRMED", total, "TRY");
        idempotency.complete(hotel.id(), key, json.write(receipt));
        TransactionLog.afterCommit(() -> log.info("event=reservation.created hotelId={} reservationId={} status=CONFIRMED serviceCount={} extrasTotal={}",
            hotel.id(), id, serviceLines.size(), total.subtract(roomTotal)));
        return receipt;
    }

    private static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
