package com.hotelbooking.events.application.port;

import com.hotelbooking.events.application.model.EventDelivery;
import com.hotelbooking.events.application.model.RetrySchedule;
import java.util.Optional;
import java.util.UUID;

public interface EventDeliveries {
    Optional<EventDelivery> lockNext(String consumer);

    void complete(UUID event, String consumer);

    Optional<RetrySchedule> fail(UUID event, String consumer, String error);
}
