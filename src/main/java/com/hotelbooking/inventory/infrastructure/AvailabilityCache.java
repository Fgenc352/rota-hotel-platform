package com.hotelbooking.inventory.infrastructure;

import com.hotelbooking.inventory.application.model.Offer;
import com.hotelbooking.inventory.application.port.AvailabilityResults;
import com.hotelbooking.shared.domain.Stay;
import com.hotelbooking.inventory.config.AvailabilityCacheProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.availability-cache.provider", havingValue = "memory", matchIfMissing = true)
@RequiredArgsConstructor
public class AvailabilityCache implements AvailabilityResults {
    private record Key(UUID hotel, Stay stay) {
    }

    private record Entry(long version, Instant expires, List<Offer> offers) {
    }

    private final AvailabilityCacheProperties properties;

    private final ConcurrentHashMap<Key, Entry> entries = new ConcurrentHashMap<>();

    public List<Offer> get(UUID hotel, Stay stay, long version) {
        Entry entry = entries.get(new Key(hotel, stay));
        return entry != null && entry.version() == version && entry.expires().isAfter(Instant.now()) ? entry.offers() : null;
    }

    public void put(UUID hotel, Stay stay, long version, List<Offer> offers) {
        if (entries.size() >= properties.maxEntries()) {
            entries.clear();
        }
        entries.put(new Key(hotel, stay), new Entry(version, Instant.now().plus(properties.ttl()), List.copyOf(offers)));
    }
}
