package com.hotelbooking.events.api;

import com.hotelbooking.events.application.command.RetryEventHandler;
import com.hotelbooking.events.application.model.FailedEvent;
import com.hotelbooking.events.application.query.ListFailedEventsHandler;
import com.hotelbooking.identity.application.model.CurrentStaff;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crm")
@Validated
@RequiredArgsConstructor
public class EventOperationsController {
    private final ListFailedEventsHandler listFailedEvents;
    private final RetryEventHandler retryEvent;

    @GetMapping("/events/failed")
    List<FailedEvent> failed(@Parameter(hidden = true) CurrentStaff staff) {
        return listFailedEvents.handle(new ListFailedEventsHandler.Input(staff.hotelId()));
    }

    @PostMapping("/events/{id}/retry")
    void retry(@Parameter(hidden = true) CurrentStaff staff, @PathVariable UUID id) {
        UUID hotel = staff.hotelId();
        retryEvent.handle(new RetryEventHandler.Input(hotel, id));
    }
}
