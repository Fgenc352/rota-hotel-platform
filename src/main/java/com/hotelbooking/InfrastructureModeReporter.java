package com.hotelbooking;

import com.hotelbooking.events.config.MessagingProperties;
import com.hotelbooking.inventory.config.AvailabilityCacheProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InfrastructureModeReporter {
    private final MessagingProperties messaging;
    private final AvailabilityCacheProperties cache;

    @EventListener(ApplicationReadyEvent.class)
    public void report() {
        log.info("event=infrastructure.mode pmsTransport={} availabilityCache={}", messaging.transport(), cache.provider());
    }
}
