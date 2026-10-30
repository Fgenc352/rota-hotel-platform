package com.hotelbooking.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelbooking.inventory.application.model.Night;
import com.hotelbooking.inventory.application.model.Offer;
import com.hotelbooking.inventory.config.AvailabilityCacheProperties;
import com.hotelbooking.inventory.infrastructure.AvailabilityCache;
import com.hotelbooking.inventory.infrastructure.RedisAvailabilityCache;
import com.hotelbooking.shared.domain.Stay;
import com.hotelbooking.shared.json.Json;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("unit")
class AvailabilityCacheTest {
    private final UUID hotel = UUID.randomUUID();
    private final Stay stay = new Stay(LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 9), 2, 0);
    private final Json json = new Json(new ObjectMapper().findAndRegisterModules());

    private AvailabilityCacheProperties properties(AvailabilityCacheProperties.Provider provider) {
        return new AvailabilityCacheProperties(provider, Duration.ofSeconds(5), 2000, "test:availability");
    }

    private List<Offer> offers() {
        return List.of(new Offer(UUID.randomUUID(), "Suite", 2, 1, new BigDecimal("115.00"), new BigDecimal("230.00"),
            "TRY", List.of(new Night(stay.checkIn(), new BigDecimal("115.00"))), 1, new BigDecimal("100.00"), new BigDecimal("1.15")));
    }

    @Test
    void memoryModePreservesTenantDatePartyAndVersionIsolation() {
        var cache = new AvailabilityCache(properties(AvailabilityCacheProperties.Provider.MEMORY));
        var result = offers();
        cache.put(hotel, stay, 1, result);
        assertThat(cache.get(hotel, stay, 1)).isEqualTo(result);
        assertThat(cache.get(hotel, stay, 2)).isNull();
        assertThat(cache.get(UUID.randomUUID(), stay, 1)).isNull();
        assertThat(cache.get(hotel, new Stay(stay.checkIn(), stay.checkOut(), 1, 0), 1)).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void redisSharesJsonBetweenInstancesAndSeparatesVersionsAndTenants() {
        var redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        Map<String, String> data = new HashMap<>();
        doAnswer(call -> {
            data.put(call.getArgument(0), call.getArgument(1));
            return null;
        })
            .when(values).set(anyString(), anyString(), eq(Duration.ofSeconds(5)));
        when(values.get(anyString())).thenAnswer(call -> data.get(call.getArgument(0)));
        var first = new RedisAvailabilityCache(redis, json, properties(AvailabilityCacheProperties.Provider.REDIS));
        var second = new RedisAvailabilityCache(redis, json, properties(AvailabilityCacheProperties.Provider.REDIS));
        var result = offers();
        first.put(hotel, stay, 1, result);
        assertThat(second.get(hotel, stay, 1)).isEqualTo(result);
        assertThat(second.get(hotel, stay, 2)).isNull();
        assertThat(second.get(UUID.randomUUID(), stay, 1)).isNull();
        assertThat(second.get(hotel, new Stay(stay.checkIn(), stay.checkOut(), 2, 1), 1)).isNull();
        verify(values).set(anyString(), anyString(), eq(Duration.ofSeconds(5)));
    }

    @Test
    @SuppressWarnings("unchecked")
    void redisOutageAndCorruptEntriesDoNotBreakAvailabilitySearch() {
        var redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        var cache = new RedisAvailabilityCache(redis, json, properties(AvailabilityCacheProperties.Provider.REDIS));
        when(values.get(anyString())).thenThrow(new RedisConnectionFailureException("offline"));
        assertThat(cache.get(hotel, stay, 1)).isNull();
        doThrow(new RedisConnectionFailureException("offline")).when(values).set(anyString(), anyString(),
            any(Duration.class));
        assertThatCode(() -> cache.put(hotel, stay, 1, offers())).doesNotThrowAnyException();
        doReturn("invalid json").when(values).get(anyString());
        assertThat(cache.get(hotel, stay, 1)).isNull();
    }
}
