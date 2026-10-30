package com.hotelbooking.pms.application.port;

import com.hotelbooking.pms.application.model.ServiceTaskAudit;
import com.hotelbooking.pms.application.model.ServiceTaskState;
import java.util.List;
import java.util.UUID;

public interface ServiceTasks {
    List<ServiceTaskState> states(UUID hotel, UUID reservation);
    List<ServiceTaskAudit> history(UUID hotel, UUID reservation);
}
