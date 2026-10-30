package com.hotelbooking.unit;

import com.hotelbooking.guest.infrastructure.entity.GuestIdentifierId;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class GuestIdentityKeyTest {
    @Test
    void emailCaseAndOuterWhitespaceDoNotCreateAnotherIdentity() {
        UUID hotel = UUID.randomUUID();
        assertThat(new GuestIdentifierId(hotel, "  GUEST@Example.com ", "+905551111111"))
            .isEqualTo(new GuestIdentifierId(hotel, "guest@example.com", "+905551111111"));
    }
}
