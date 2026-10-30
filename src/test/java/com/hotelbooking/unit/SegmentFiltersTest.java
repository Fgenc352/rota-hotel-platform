package com.hotelbooking.unit;

import com.hotelbooking.guest.application.model.Segment;
import com.hotelbooking.guest.application.port.SegmentQueries;
import com.hotelbooking.guest.infrastructure.segment.*;
import com.hotelbooking.shared.exception.BusinessException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@Tag("unit")
class SegmentFiltersTest {
    private final UUID hotel = UUID.randomUUID();

    @Test
    void builtInFiltersKeepTheirCheckoutAndConsentRules() {
        var filters = new GuestSegmentFilters(List.of(new AllGuestsFilter(), new ReturningGuestsFilter(), new ReengageGuestsFilter()));
        assertThat(filters.condition(hotel, "ALL", new HashMap<String, Object>())).isEmpty();
        assertThat(filters.condition(hotel, "RETURNING", new HashMap<String, Object>()))
            .contains("CHECKED_OUT", "checked_out_at", "12 months", ">= 2", "r.hotel_id=g.hotel_id");
        assertThat(filters.condition(hotel, "REENGAGE", new HashMap<String, Object>()))
            .contains("g.marketing_allowed", "checked_out_at", " - 30", "r.hotel_id=g.hotel_id");
    }

    @Test
    void newFilterCanBeRegisteredWithoutChangingTheRegistryOrRepository() {
        SegmentFilter custom = new SegmentFilter() {
            public boolean supports(String key) {
                return "CUSTOM".equals(key);
            }
            public String condition(UUID tenant, String key, Map<String, Object> params) {
                params.put("customValue", "parameterized");
                return " AND g.id=:customValue";
            }
        };
        var filters = new GuestSegmentFilters(List.of(new AllGuestsFilter(), custom));
        var parameters = new HashMap<String, Object>();
        assertThat(filters.condition(hotel, "CUSTOM", parameters)).isEqualTo(" AND g.id=:customValue");
        assertThat(parameters.get("customValue")).isEqualTo("parameterized");
    }

    @Test
    void ambiguousOrUnknownKeysAreRejected() {
        var duplicates = new GuestSegmentFilters(List.of(new AllGuestsFilter(), new AllGuestsFilter()));
        assertThatThrownBy(() -> duplicates.condition(hotel, "ALL", new HashMap<String, Object>())).isInstanceOf(IllegalStateException.class);
        var filters = new GuestSegmentFilters(List.of(new AllGuestsFilter()));
        assertThatThrownBy(() -> filters.condition(hotel, "UNKNOWN", new HashMap<String, Object>())).isInstanceOf(BusinessException.class);
    }

    @Test
    void savedRuleUsesTenantLookupAndBoundParameters() {
        UUID segment = UUID.randomUUID();
        SegmentQueries queries = new SegmentQueries() {
            public List<Segment> segments(UUID tenant) {
                return List.of();
            }
            public Optional<Segment> find(UUID tenant, UUID id) {
                assertThat(tenant).isEqualTo(hotel);
                assertThat(id).isEqualTo(segment);
                return Optional.of(new Segment(id, tenant, "private rule", 2, 12, true));
            }
        };
        var filter = new SavedSegmentFilter(queries);
        var params = new HashMap<String, Object>();
        assertThat(filter.supports(segment.toString())).isTrue();
        assertThat(filter.supports("not-a-uuid")).isFalse();
        assertThat(filter.condition(hotel, segment.toString(), params)).contains(":months", ":min",
            "g.marketing_allowed");
        assertThat(params.get("months")).isEqualTo(12);
        assertThat(params.get("min")).isEqualTo(2);
        assertThat(params).doesNotContainKey("days");
    }
}
