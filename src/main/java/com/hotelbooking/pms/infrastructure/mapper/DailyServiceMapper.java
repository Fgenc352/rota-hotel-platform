package com.hotelbooking.pms.infrastructure.mapper;

import com.hotelbooking.pms.application.model.DailyServiceTask;
import com.hotelbooking.pms.application.model.DailyServiceAudit;
import com.hotelbooking.pms.infrastructure.entity.PmsDailyServiceEntity;
import com.hotelbooking.pms.infrastructure.entity.PmsDailyServiceAuditEntity;
import org.springframework.stereotype.Component;

@Component
public class DailyServiceMapper {
    public DailyServiceTask task(PmsDailyServiceEntity row) {
        return new DailyServiceTask(row.getReservationId(), row.getCode(), row.getDate(), row.getStatus(),
            row.getTime(),
            row.getPickup(), row.getDestination(), row.getPlate(), row.getNotes(), row.getPassengers(), row.getActor(),
            row.getUpdatedAt());
    }
    public DailyServiceAudit audit(PmsDailyServiceAuditEntity row) {
        return new DailyServiceAudit(row.getId(), row.getActor(), row.getBeforeState(), row.getAfterState(),
            row.getCreatedAt());
    }
}
