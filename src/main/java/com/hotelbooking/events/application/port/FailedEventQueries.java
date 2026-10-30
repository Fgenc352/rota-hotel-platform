package com.hotelbooking.events.application.port;

import com.hotelbooking.events.application.model.FailedEvent;
import java.util.List;
import java.util.UUID;

public interface FailedEventQueries {
    List<FailedEvent> failed(UUID hotel);
}
