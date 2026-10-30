package com.hotelbooking.pms.application.command;

import com.hotelbooking.pms.application.port.RoomOperations;
import com.hotelbooking.pms.domain.Housekeeping;
import com.hotelbooking.pms.domain.RoomRules;
import com.hotelbooking.shared.exception.BusinessException;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.MANDATORY)
@RequiredArgsConstructor
public class ApplyStayTransitionHandler {
    public record Input(UUID hotel, UUID reservation, String status, String actor) {
    }
    private final RoomOperations rooms;

    public void handle(Input input) {
        var assignment = rooms.assignment(input.hotel(), input.reservation());
        // Existing CRM reservations can still check in without a physical assignment.
        if (assignment.isEmpty()) {
            return;
        }
        var current = assignment.get();
        var room = rooms.lockRoom(input.hotel(), current.roomId());
        if (input.status().equals("CHECKED_IN")) {
            RoomRules.readyForCheckIn(room);
            if (rooms.conflicts(input.hotel(), room.id(), input.reservation(), current.checkIn(), current.checkOut(),
                true)) {
                throw BusinessException.conflict("ROOM_OCCUPIED", "Atanan odada halen başka bir misafir var.");
            }
        }
        rooms.transition(input.hotel(), input.reservation(), input.status());
        if (input.status().equals("CHECKED_OUT")) {
            rooms.housekeeping(input.hotel(), room.id(), Housekeeping.DIRTY);
        }
        rooms.audit(input.hotel(), room.id(), input.reservation(), "STAY_STATUS_CHANGED", input.actor(),
            Map.of("status", current.status(), "housekeeping", room.housekeeping()),
            Map.of("status", input.status(), "housekeeping", input.status().equals("CHECKED_OUT") ? Housekeeping.DIRTY : room.housekeeping()));
    }
}
