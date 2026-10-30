package com.hotelbooking.events.infrastructure;

import com.hotelbooking.contracts.reservation.ReservationSnapshot;
import com.hotelbooking.events.application.EventConsumer;
import com.hotelbooking.events.application.PmsGateway;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PmsEventConsumer implements EventConsumer {
    private final PmsGateway gateway;

    @Override
    public String name() {
        return "PMS";
    }

    @Override
    public boolean requiresLocalTransaction() {
        return false;
    }

    @Override
    public void accept(UUID event, String type, String traceId, ReservationSnapshot snapshot) {
        if (type.equals("ReservationCreated") || type.equals("ReservationStatusChanged") || type.equals("ReservationCancelled")) {
            gateway.deliver(event, traceId, snapshot);
        }
    }
}
