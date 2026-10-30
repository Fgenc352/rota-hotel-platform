package com.hotelbooking.events.infrastructure.mapper;

import com.hotelbooking.events.application.model.EventDelivery;
import com.hotelbooking.events.application.model.FailedEvent;
import com.hotelbooking.events.application.model.RetrySchedule;
import com.hotelbooking.events.infrastructure.entity.EventDeliveryEntity;
import org.springframework.stereotype.Component;

@Component
public class EventDeliveryEntityMapper {
    public EventDelivery toModel(EventDeliveryEntity entity) {
        var event = entity.getEvent();
        return new EventDelivery(event.getId(), event.getEventType(), event.getTraceId(), event.getPayload());
    }

    public RetrySchedule toRetry(EventDeliveryEntity entity) {
        return new RetrySchedule(entity.getAttempts(), entity.isDead(), entity.getNextAttempt());
    }

    public FailedEvent toFailed(EventDeliveryEntity entity) {
        var event = entity.getEvent();
        return new FailedEvent(event.getId(), event.getEventType(), event.getTraceId(), entity.getId().getConsumer(),
            entity.getAttempts(), entity.getLastError());
    }
}
