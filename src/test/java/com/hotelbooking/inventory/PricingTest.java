package com.hotelbooking.inventory;

import com.hotelbooking.inventory.domain.Room;
import com.hotelbooking.inventory.application.mapper.OfferMapper;
import com.hotelbooking.inventory.application.mapper.NightMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import com.hotelbooking.shared.domain.Stay;

import static org.assertj.core.api.Assertions.*;

@Tag("unit")
class PricingTest {

    @ParameterizedTest
    @CsvSource({
        "2026-11-02,3200.00", "2026-11-03,3200.00", "2026-11-04,3200.00",
        "2026-11-05,3200.00", "2026-11-06,3200.00", "2026-11-07,3680.00",
        "2026-11-08,3680.00"
    })
    void demoTariffChargesOnlySaturdayAndSunday(LocalDate date, BigDecimal expected) {
        assertThat(demoRoom().price(date)).isEqualByComparingTo(expected);
    }

    @Test
    void mixedStayChargesEachNightAndExcludesCheckoutDay() {
        var friday = LocalDate.of(2026, 11, 6);
        assertThat(demoRoom().total(new Stay(friday, friday.plusDays(3), 2, 0)))
            .isEqualByComparingTo("10560.00");
        assertThat(demoRoom().total(new Stay(friday, friday.plusDays(1), 2, 0)))
            .isEqualByComparingTo("3200.00");
        assertThat(demoRoom().total(new Stay(friday.plusDays(1), friday.plusDays(2), 2, 0)))
            .isEqualByComparingTo("3680.00");
    }

    @Test
    void quoteExposesTariffAndPreservesTheAuthoritativeNightlyTotals() {
        var friday = LocalDate.of(2026, 11, 6);
        var quote = new OfferMapper(new NightMapper()).toOffer(demoRoom(),
            new Stay(friday, friday.plusDays(3), 2, 0), 0);
        assertThat(quote.baseNightlyPrice()).isEqualByComparingTo("3200.00");
        assertThat(quote.weekendMultiplier()).isEqualByComparingTo("1.15");
        assertThat(quote.total()).isEqualByComparingTo("10560.00");
        assertThat(quote.nights()).extracting(night -> night.price())
            .containsExactly(new BigDecimal("3200.00"), new BigDecimal("3680.00"), new BigDecimal("3680.00"));
    }

    private Room demoRoom() {
        return new Room(UUID.randomUUID(), UUID.randomUUID(), "Standart", 2, 20,
            new BigDecimal("3200.00"), new BigDecimal("1.15"), 1);
    }
}
