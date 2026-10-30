package com.hotelbooking.pms.infrastructure.mapper;

import com.hotelbooking.pms.application.model.ServiceTaskAudit;
import com.hotelbooking.pms.application.model.ServiceTaskState;
import com.hotelbooking.pms.infrastructure.entity.PmsServiceAuditEntity;
import com.hotelbooking.pms.infrastructure.entity.PmsServiceTaskEntity;
import org.springframework.stereotype.Component;

@Component
public class PmsServiceMapper {
    public ServiceTaskState state(PmsServiceTaskEntity entity) {
        return new ServiceTaskState(entity.getCode(), entity.getProgress(), entity.getUpdatedBy(), entity.getUpdatedAt());
    }

    public ServiceTaskAudit audit(PmsServiceAuditEntity entity) {
        return new ServiceTaskAudit(entity.getId(), entity.getCode(), entity.getPreviousState(), entity.getNextState(),
            entity.getActor(), entity.getCreatedAt());
    }
}
