package com.hotelbooking.guest.infrastructure;

import com.hotelbooking.contracts.reservation.ReservationSnapshot;
import com.hotelbooking.events.application.EventConsumer;
import com.hotelbooking.guest.application.command.ApplyReservationProjectionHandler;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CrmEventConsumer implements EventConsumer {
    private final ApplyReservationProjectionHandler projection;

    public String name() {
        return "CRM";
    }
    public void accept(UUID event, String type, String traceId, ReservationSnapshot snapshot) {
        projection.handle(new ApplyReservationProjectionHandler.Input(snapshot));
    }
}
