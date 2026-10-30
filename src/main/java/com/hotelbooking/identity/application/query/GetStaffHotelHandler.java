package com.hotelbooking.identity.application.query;

import com.hotelbooking.identity.application.port.StaffAccounts;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetStaffHotelHandler {
    public record Input(String username) {
    }

    private final StaffAccounts staff;

    public UUID handle(Input input) {
        return staff.hotel(input.username());
    }
}
