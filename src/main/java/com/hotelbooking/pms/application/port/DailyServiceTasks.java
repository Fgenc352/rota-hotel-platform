package com.hotelbooking.pms.application.port;

import com.hotelbooking.pms.application.model.DailyServiceTask;
import com.hotelbooking.pms.application.model.DailyServiceAudit;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DailyServiceTasks {
    List<DailyServiceTask> tasks(UUID hotel, Collection<UUID> reservations, LocalDate date);
    void save(UUID hotel, DailyServiceTask value);
    List<DailyServiceAudit> history(UUID hotel, UUID reservation, String code, LocalDate date);
}
