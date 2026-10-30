package com.hotelbooking.unit;

import com.hotelbooking.reservation.domain.*;
import com.hotelbooking.shared.domain.Stay;
import com.hotelbooking.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

@Tag("unit")
class ServicePricingTest {
    private final Stay stay = new Stay(LocalDate.of(2027, 2, 10), LocalDate.of(2027, 2, 13), 2, 1);
    private final List<ServiceOption> catalog = List.of(
        new ServiceOption("BREAKFAST", "Kahvaltı", "", BigDecimal.ZERO, ServiceUnit.PER_PERSON_PER_NIGHT, true),
        new ServiceOption("LUNCH", "Öğle yemeği", "", new BigDecimal("650.00"), ServiceUnit.PER_PERSON_PER_NIGHT,
        false),
        new ServiceOption("VALET", "Vale", "", new BigDecimal("300.00"), ServiceUnit.PER_NIGHT, false),
        new ServiceOption("TRANSFER", "Transfer", "", new BigDecimal("1800.00"), ServiceUnit.PER_STAY, false));

    @Test void includedServicesAreSnapshottedWithoutCharging() {
        var lines = ServicePricing.quote(catalog, List.of(), stay);
        assertThat(lines).hasSize(1);
        assertThat(lines.getFirst().included()).isTrue();
        assertThat(lines.getFirst().quantity()).isEqualTo(9);
        assertThat(lines.getFirst().total()).isEqualByComparingTo("0");
    }
    @Test void quantityUsesPeopleNightsAndPerStayAndPreservesPrices() {
        var lines = ServicePricing.quote(catalog, List.of("LUNCH", "VALET", "TRANSFER"), stay);
        assertThat(lines.stream().map(ServiceLine::total).reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("8550.00");
        assertThat(lines.stream().filter(line -> line.code().equals("LUNCH")).findFirst().orElseThrow().quantity()).isEqualTo(9);
        assertThat(lines.stream().filter(line -> line.code().equals("VALET")).findFirst().orElseThrow().quantity()).isEqualTo(3);
    }
    @Test void unknownOrDuplicateSelectionsAreRejected() {
        assertThatThrownBy(() -> ServicePricing.quote(catalog, List.of("OTHER_HOTEL_SERVICE"), stay)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> ServicePricing.quote(catalog, List.of("VALET", "VALET"), stay)).isInstanceOf(BusinessException.class);
    }
    @Test void selectedOrderDoesNotAffectSnapshotOrTotal() {
        assertThat(ServicePricing.quote(catalog, List.of("VALET", "LUNCH"), stay))
            .isEqualTo(ServicePricing.quote(catalog, List.of("LUNCH", "VALET"), stay));
    }
}
