package com.hotelbooking.pms.application.command;

import com.hotelbooking.inventory.application.port.InventoryCommands;
import com.hotelbooking.pms.application.port.RoomOperations;
import com.hotelbooking.pms.domain.Housekeeping;
import com.hotelbooking.pms.domain.PhysicalRoom;
import com.hotelbooking.pms.domain.RoomAssignment;
import com.hotelbooking.pms.domain.RoomRules;
import com.hotelbooking.reservation.application.port.Reservations;
import com.hotelbooking.shared.exception.BusinessException;
import com.hotelbooking.shared.logging.TransactionLog;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AssignRoomHandler {
    public record Input(UUID hotel, UUID reservation, UUID room, String actor) {
    }
    private final Reservations reservations;
    private final InventoryCommands inventory;
    private final RoomOperations rooms;

    public void handle(Input input) {
        UUID type = reservations.roomId(input.hotel(), input.reservation());
        inventory.room(input.hotel(), type);
        var state = reservations.lock(input.hotel(), input.reservation());
        var previous = rooms.assignment(input.hotel(), input.reservation());
        Map<UUID, PhysicalRoom> locked = new HashMap<>();
        // Keep reservation lock order and lock old/new physical rooms in stable UUID order.
        Stream.concat(Stream.of(input.room()), previous.stream().map(RoomAssignment::roomId)).distinct().sorted()
            .forEach(id -> locked.put(id, rooms.lockRoom(input.hotel(), id)));
        var target = locked.get(input.room());
        RoomRules.assignable(target, type, state.status().name());
        if (rooms.conflicts(input.hotel(), target.id(), input.reservation(), state.checkIn(), state.checkOut(),
            state.status().name().equals("CHECKED_IN"))) {
            throw BusinessException.conflict("ROOM_OCCUPIED", "Bu oda seçilen konaklamayla çakışıyor veya halen dolu. Başka oda seçin.");
        }
        if (previous.isPresent() && previous.get().roomId().equals(target.id())) {
            return;
        }
        if (state.status().name().equals("CHECKED_IN")) {
            RoomRules.readyForCheckIn(target);
        }
        rooms.assign(input.hotel(), new RoomAssignment(input.reservation(), target.id(), state.code(), state.checkIn(),
            state.checkOut(), state.status().name()));
        if (previous.isPresent() && state.status().name().equals("CHECKED_IN")) {
            var oldRoom = locked.get(previous.get().roomId());
            rooms.housekeeping(input.hotel(), oldRoom.id(), Housekeeping.DIRTY);
            rooms.audit(input.hotel(), oldRoom.id(), input.reservation(), "ROOM_VACATED", input.actor(),
                Map.of("housekeeping", oldRoom.housekeeping()), Map.of("housekeeping", Housekeeping.DIRTY));
        }
        var before = Map.of("room", previous.map(value -> locked.get(value.roomId()).number()).orElse("Atanmamış"));
        rooms.audit(input.hotel(), target.id(), input.reservation(), "ROOM_ASSIGNED", input.actor(), before,
            Map.of("room", target.number()));
        TransactionLog.afterCommit(() -> log.info("event=pms.room_assigned hotelId={} reservationId={} roomId={}",
            input.hotel(), input.reservation(), target.id()));
    }
}
