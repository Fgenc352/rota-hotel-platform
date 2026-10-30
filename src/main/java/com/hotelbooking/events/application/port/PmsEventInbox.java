package com.hotelbooking.events.application.port;

import com.hotelbooking.events.application.model.BrokerReservationEvent;

public interface PmsEventInbox {
    boolean stage(BrokerReservationEvent event);
}
