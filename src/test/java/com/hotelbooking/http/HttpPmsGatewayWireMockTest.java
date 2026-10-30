package com.hotelbooking.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.hotelbooking.contracts.reservation.ReservationSnapshot;
import com.hotelbooking.events.application.mapper.PmsMessageMapper;
import com.hotelbooking.events.config.PmsProperties;
import com.hotelbooking.events.infrastructure.HttpPmsGateway;
import com.hotelbooking.shared.json.Json;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.*;

@Tag("http")
class HttpPmsGatewayWireMockTest {
    private static final UUID EVENT = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private final WireMockServer pms = new WireMockServer(wireMockConfig()
        .dynamicPort().usingFilesUnderClasspath("wiremock/pms"));
    private final Json json = new Json(new ObjectMapper().findAndRegisterModules()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));
    private ReservationSnapshot snapshot;

    @BeforeEach
    void start() throws IOException {
        snapshot = json.read(fixture("reservation-snapshot.json"), ReservationSnapshot.class);
        pms.start();
    }

    @AfterEach
    void stop() {
        if (pms.isRunning()) {
            pms.stop();
        }
    }

    @Test
    void acceptedRequestHasStableIdentityTraceAndOnlyContractMetadata() throws IOException {
        gateway(Duration.ofSeconds(3)).deliver(EVENT, "wiremock-trace", snapshot);
        pms.verify(1, postRequestedFor(urlEqualTo("/reservations"))
            .withHeader("Idempotency-Key", equalTo(EVENT.toString()))
            .withHeader("X-Trace-Id", equalTo("wiremock-trace"))
            .withRequestBody(equalToJson(fixture("expected-message.json"))));
        assertThat(pms.findAllUnmatchedRequests()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"unavailable", "rate-limited"})
    void nonSuccessfulResponsesRemainFailuresForTheOutboxToRetry(String state) {
        pms.setScenarioState("pms-delivery", state);
        assertThatThrownBy(() -> gateway(Duration.ofSeconds(3)).deliver(EVENT, "wiremock-trace", snapshot))
            .isInstanceOf(IllegalStateException.class).hasMessage("PMS rejected request");
        pms.verify(1, postRequestedFor(urlEqualTo("/reservations")));
        assertThat(pms.findAllUnmatchedRequests()).isEmpty();
    }

    @Test
    void delayedResponseUsesTheRealHttpClientTimeout() {
        pms.setScenarioState("pms-delivery", "slow");
        assertThatThrownBy(() -> gateway(Duration.ofMillis(500)).deliver(EVENT, "wiremock-trace", snapshot))
            .isInstanceOf(IllegalStateException.class).hasMessage("PMS unavailable")
            .hasCauseInstanceOf(HttpTimeoutException.class);
        pms.verify(1, postRequestedFor(urlEqualTo("/reservations")));
    }

    @Test
    void retryAfterRecoveryKeepsTheSameEventIdAndPayload() throws IOException {
        pms.setScenarioState("pms-delivery", "recovering");
        var gateway = gateway(Duration.ofSeconds(3));
        assertThatThrownBy(() -> gateway.deliver(EVENT, "wiremock-trace", snapshot))
            .isInstanceOf(IllegalStateException.class).hasMessage("PMS rejected request");
        gateway.deliver(EVENT, "wiremock-trace", snapshot);
        pms.verify(2, postRequestedFor(urlEqualTo("/reservations"))
            .withHeader("Idempotency-Key", equalTo(EVENT.toString()))
            .withRequestBody(equalToJson(fixture("expected-message.json"))));
        assertThat(pms.findAllUnmatchedRequests()).isEmpty();
    }

    private HttpPmsGateway gateway(Duration timeout) {
        var properties = new PmsProperties(URI.create(pms.baseUrl()), Duration.ofSeconds(1), timeout);
        return new HttpPmsGateway(json, properties, HttpClient.newBuilder()
            .connectTimeout(properties.connectTimeout()).build(), new PmsMessageMapper());
    }

    private static String fixture(String name) throws IOException {
        try (var input = HttpPmsGatewayWireMockTest.class.getResourceAsStream("/wiremock/pms/__files/" + name)) {
            if (input == null) {
                throw new IOException("Missing PMS fixture: " + name);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
