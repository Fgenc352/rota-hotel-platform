package com.hotelbooking.pms.api.mapper;

import com.hotelbooking.identity.application.model.CurrentStaff;
import com.hotelbooking.pms.api.dto.DailyServiceRequest;
import com.hotelbooking.pms.application.command.UpdateDailyServiceHandler;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DailyServiceCommandMapper {
    public UpdateDailyServiceHandler.Input command(CurrentStaff staff, UUID reservation, String code, DailyServiceRequest request) {
        return new UpdateDailyServiceHandler.Input(staff.hotelId(), reservation, code, request.date(), request.status(),
            request.time(),
            request.pickup(), request.destination(), request.plate(), request.notes(), request.passengers(), staff.username());
    }
}
