package com.hotelbooking.inventory.infrastructure;

import com.hotelbooking.inventory.application.model.Offer;
import com.hotelbooking.inventory.application.port.AvailabilityResults;
import com.hotelbooking.inventory.config.AvailabilityCacheProperties;
import com.hotelbooking.shared.domain.Stay;
import com.hotelbooking.shared.json.Json;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.availability-cache.provider", havingValue = "redis")
@RequiredArgsConstructor
@Slf4j
public class RedisAvailabilityCache implements AvailabilityResults {
    private final StringRedisTemplate redis;
    private final Json json;
    private final AvailabilityCacheProperties properties;
    private final AtomicBoolean unavailable = new AtomicBoolean();

    @Override
    public List<Offer> get(UUID hotel, Stay stay, long version) {
        try {
            String value = redis.opsForValue().get(key(hotel, stay, version));
            recovered();
            if (value == null) {
                return null;
            }
            try {
                return List.copyOf(Arrays.asList(json.read(value, Offer[].class)));
            } catch (IllegalStateException | NullPointerException malformed) {
                log.warn("event=availability.cache_invalid provider=redis hotelId={}", hotel);
                return null;
            }
        } catch (DataAccessException unavailableRedis) {
            failed("get", unavailableRedis);
            return null;
        }
    }

    @Override
    public void put(UUID hotel, Stay stay, long version, List<Offer> offers) {
        try {
            redis.opsForValue().set(key(hotel, stay, version), json.write(offers), properties.ttl());
            recovered();
        } catch (DataAccessException unavailableRedis) {
            failed("put", unavailableRedis);
        }
    }

    private String key(UUID hotel, Stay stay, long version) {
        return "%s:%s:%d:%s:%s:%d:%d".formatted(properties.namespace(), hotel, version,
            stay.checkIn(), stay.checkOut(), stay.adults(), stay.children());
    }

    private void failed(String operation, Exception failure) {
        if (unavailable.compareAndSet(false, true)) {
            log.warn("event=availability.cache_unavailable provider=redis operation={} errorType={} fallback=database",
                operation, failure.getClass().getSimpleName());
        }
    }

    private void recovered() {
        if (unavailable.compareAndSet(true, false)) {
            log.info("event=availability.cache_recovered provider=redis");
        }
    }
}
