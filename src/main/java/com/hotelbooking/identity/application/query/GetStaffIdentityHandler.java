package com.hotelbooking.identity.application.query;

import com.hotelbooking.identity.application.model.StaffIdentity;
import com.hotelbooking.identity.application.port.StaffAccounts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetStaffIdentityHandler {
    public record Input(String username) {
    }

    private final StaffAccounts staff;

    public StaffIdentity handle(Input input) {
        return staff.identity(input.username());
    }
}
