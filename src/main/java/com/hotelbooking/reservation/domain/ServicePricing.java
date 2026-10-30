package com.hotelbooking.reservation.domain;

import com.hotelbooking.shared.domain.Stay;
import com.hotelbooking.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public final class ServicePricing {
    private ServicePricing() {
    }

    public static List<ServiceLine> quote(List<ServiceOption> catalog, List<String> selected, Stay stay) {
        if (selected.size() > 20 || Set.copyOf(selected).size() != selected.size()) {
            throw BusinessException.invalid("Hizmet seçimleri benzersiz olmalıdır; en fazla 20 hizmet seçilebilir.");
        }
        var codes = Set.copyOf(selected);
        if (codes.stream().anyMatch(code -> catalog.stream().noneMatch(option -> option.code().equals(code)))) {
            throw BusinessException.invalid("Seçilen hizmet bu otelde sunulmuyor.");
        }
        return catalog.stream()
            .filter(option -> option.included() || codes.contains(option.code()))
            .map(option -> line(option, stay))
            .toList();
    }

    private static ServiceLine line(ServiceOption option, Stay stay) {
        long quantity = option.unit().quantity(stay);
        var unitPrice = option.included() ? BigDecimal.ZERO : option.price();
        return new ServiceLine(option.code(), option.name(), option.unit(), unitPrice, quantity,
            unitPrice.multiply(BigDecimal.valueOf(quantity)), option.included());
    }
}
