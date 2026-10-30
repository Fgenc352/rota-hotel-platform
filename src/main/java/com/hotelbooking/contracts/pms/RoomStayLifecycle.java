package com.hotelbooking.contracts.pms;

import java.util.UUID;

/** Participates in the reservation command transaction, keeping physical room operations consistent. */
public interface RoomStayLifecycle {
    void transition(UUID hotel, UUID reservation, String nextStatus);
}
