package com.hotelbooking.guest.api.mapper;

import com.hotelbooking.guest.api.dto.AddTagRequest;
import com.hotelbooking.guest.application.command.AddGuestTagHandler;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GuestTagRequestMapper {
    public AddGuestTagHandler.Input toCommand(UUID hotel, UUID guest, AddTagRequest request) {
        return new AddGuestTagHandler.Input(hotel, guest, request.tag());
    }
}
