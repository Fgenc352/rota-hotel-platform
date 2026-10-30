package com.hotelbooking.events.infrastructure;

import com.hotelbooking.contracts.reservation.ReservationSnapshot;
import com.hotelbooking.events.application.PmsGateway;
import com.hotelbooking.events.config.PmsProperties;
import com.hotelbooking.shared.json.Json;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.hotelbooking.events.application.mapper.PmsMessageMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class HttpPmsGateway implements PmsGateway {
    private final Json json;

    private final PmsProperties properties;
    private final HttpClient http;
    private final PmsMessageMapper mapper;

    @Override
    public void deliver(UUID eventId, String traceId, ReservationSnapshot snapshot) {
        long started = System.nanoTime();
        try {
            // PMS only receives the data required by the mock contract. Event ID deduplicates retries.
            String body = json.write(mapper.toMessage(eventId, snapshot));
            String endpoint = properties.url().toString().replaceAll("/+$", "") + "/reservations";
            var req = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(properties.requestTimeout())
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", eventId.toString())
                .header("X-Trace-Id", traceId)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            log.info("event=pms.request_started eventId={} reservationId={}", eventId, snapshot.id());
            int status = http.send(req, HttpResponse.BodyHandlers.discarding()).statusCode();
            log.info("event=pms.request_completed eventId={} status={} durationMs={}", eventId, status, (System.nanoTime() - started) / 1_000_000);
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("PMS rejected request");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("PMS interrupted", e);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("PMS unavailable", e);
        }
    }
}
