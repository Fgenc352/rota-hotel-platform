package com.hotelbooking.pms.application.port;

import com.hotelbooking.pms.domain.Housekeeping;
import com.hotelbooking.pms.domain.PhysicalRoom;
import com.hotelbooking.pms.domain.RoomAssignment;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface RoomOperations {
    PhysicalRoom lockRoom(UUID hotel, UUID room);
    Optional<RoomAssignment> assignment(UUID hotel, UUID reservation);
    boolean conflicts(UUID hotel, UUID room, UUID excludeReservation, LocalDate checkIn, LocalDate checkOut,
        boolean checkingIn);
    void assign(UUID hotel, RoomAssignment assignment);
    void transition(UUID hotel, UUID reservation, String status);
    void housekeeping(UUID hotel, UUID room, Housekeeping state);
    void audit(UUID hotel, UUID room, UUID reservation, String action, String actor, Object before, Object after);
}
