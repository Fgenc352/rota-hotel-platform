package com.hotelbooking.reservation.api.mapper;

import com.hotelbooking.reservation.api.dto.FunnelRequest;
import com.hotelbooking.reservation.application.command.RecordFunnelHandler;
import org.springframework.stereotype.Component;

@Component
public class FunnelRequestMapper {
    public RecordFunnelHandler.Input toCommand(String hotelKey, FunnelRequest request) {
        return new RecordFunnelHandler.Input(hotelKey, request.sessionId(), request.stage());
    }
}
