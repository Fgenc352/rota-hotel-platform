package com.hotelbooking.events.application.command;

import com.hotelbooking.events.application.port.EventRetries;
import com.hotelbooking.shared.exception.BusinessException;
import com.hotelbooking.shared.logging.TransactionLog;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RetryEventHandler {
    public record Input(UUID hotel, UUID id) {
    }

    private final EventRetries repository;

    @Transactional
    public void handle(Input input) {
        if (!repository.retry(input.hotel(), input.id())) {
            throw BusinessException.missing();
        }
        TransactionLog.afterCommit(() -> log.info("event=outbox.retry_requested hotelId={} eventId={}", input.hotel(),
            input.id()));
    }
}
