package com.hotelbooking.identity.application.port;

import com.hotelbooking.identity.application.model.StaffCredentials;
import com.hotelbooking.identity.application.model.StaffIdentity;
import java.util.Optional;
import java.util.UUID;

public interface StaffAccounts {
    StaffIdentity identity(String username);

    UUID hotel(String username);

    Optional<StaffCredentials> credentials(String username);

    void saveDemoAccount(String username, String passwordHash, UUID hotel);
}
