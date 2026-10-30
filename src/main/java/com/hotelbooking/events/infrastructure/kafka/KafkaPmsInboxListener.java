package com.hotelbooking.events.infrastructure.kafka;

import com.hotelbooking.events.application.model.BrokerReservationEvent;
import com.hotelbooking.events.application.port.PmsEventInbox;
import com.hotelbooking.shared.json.Json;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.messaging.transport", havingValue = "kafka")
@RequiredArgsConstructor
@Slf4j
public class KafkaPmsInboxListener {
    private final Json json;
    private final PmsEventInbox inbox;

    @KafkaListener(topics = "${app.messaging.topic:hotel.reservations.v1}",
        groupId = "${app.messaging.group-id:hotel-pms-dispatch-v1}", containerFactory = "pmsKafkaListenerFactory")
    public void receive(String payload) {
        var event = json.read(payload, BrokerReservationEvent.class);
        String previous = MDC.get("traceId");
        try {
            MDC.put("traceId", event.traceId());
            boolean staged = inbox.stage(event);
            // stage() commits before this method returns and the RECORD offset can be committed.
            log.info("event=kafka.pms_staged eventId={} duplicate={}", event.eventId(), !staged);
        } finally {
            if (previous == null) {
                MDC.remove("traceId");
            } else {
                MDC.put("traceId", previous);
            }
        }
    }
}
