package com.hotelbooking.pms.domain;

import com.hotelbooking.shared.exception.BusinessException;
import java.time.LocalDate;
import java.util.List;

public final class DailyServiceRules {
    private DailyServiceRules() {
    }

    public static LocalDate end(LocalDate planned, java.time.Instant checkedOutAt, java.time.ZoneId timezone) {
        if (checkedOutAt == null) return planned;
        LocalDate actual = checkedOutAt.atZone(timezone).toLocalDate();
        return actual.isBefore(planned) ? actual : planned;
    }

    public static void operationDate(LocalDate day) {
        if (day == null || day.getYear() < 1 || day.getYear() > 9999) {
            throw BusinessException.invalid("Geçerli bir operasyon tarihi seçin.");
        }
    }

    public static List<DailyServiceStatus> workflow(String code) {
        return switch (code) {
            case "BREAKFAST", "LUNCH", "DINNER" -> List.of(DailyServiceStatus.PENDING, DailyServiceStatus.PREPARING,
                DailyServiceStatus.SERVED);
            case "TRANSFER" -> List.of(DailyServiceStatus.PENDING, DailyServiceStatus.PLANNED, DailyServiceStatus.EN_ROUTE,
                DailyServiceStatus.PICKED_UP, DailyServiceStatus.COMPLETED);
            case "VALET" -> List.of(DailyServiceStatus.PENDING, DailyServiceStatus.RECEIVED, DailyServiceStatus.PARKED,
                DailyServiceStatus.REQUESTED, DailyServiceStatus.DELIVERED);
            default -> List.of();
        };
    }

    public static boolean due(String code, LocalDate day, LocalDate in, LocalDate out) {
        return switch (code) {
            case "BREAKFAST" -> day.isAfter(in) && !day.isAfter(out);
            case "VALET" -> !day.isBefore(in) && !day.isAfter(out);
            case "LUNCH", "DINNER" -> !day.isBefore(in) && day.isBefore(out);
            case "TRANSFER" -> day.equals(in);
            default -> false;
        };
    }

    public static void date(String code, LocalDate day, LocalDate in, LocalDate out) {
        operationDate(day);
        boolean valid = code.equals("TRANSFER") ? !day.isBefore(in.minusDays(7)) && !day.isAfter(out.plusDays(1)) : due(code,
            day, in, out);
        if (!valid) throw BusinessException.invalid("Hizmet tarihi konaklama paketinin kapsamı dışında.");
    }

    public static void transition(String code, DailyServiceStatus previous, DailyServiceStatus next) {
        var states = workflow(code);
        int from = states.indexOf(previous), to = states.indexOf(next);
        if (from < 0 || to < 0 || to < from || (to > from + 1 && !(code.equals("BREAKFAST") || code.equals("LUNCH") || code.equals("DINNER")))) {
            throw BusinessException.conflict("INVALID_DAILY_SERVICE_TRANSITION", "Hizmet adımlarını sırayla ilerletin; tamamlanmış işlem geriye alınamaz.");
        }
    }
}
