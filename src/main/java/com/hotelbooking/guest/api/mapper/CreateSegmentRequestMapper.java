package com.hotelbooking.guest.api.mapper;

import com.hotelbooking.guest.api.dto.CreateSegmentRequest;
import com.hotelbooking.guest.application.command.CreateSegmentHandler;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CreateSegmentRequestMapper {
    public CreateSegmentHandler.Input toCommand(UUID hotel, CreateSegmentRequest request) {
        return new CreateSegmentHandler.Input(hotel, request.name(), request.minStays(), request.months(),
            request.marketingRequired());
    }
}
