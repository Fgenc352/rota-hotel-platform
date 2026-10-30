package com.hotelbooking.identity.infrastructure;

import com.hotelbooking.identity.config.DemoProperties;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DemoStaffInitializer implements ApplicationRunner {
    private final StaffRepository staff;
    private final PasswordEncoder encoder;

    private final DemoProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        staff.saveDemoAccount("lagos", encoder.encode(properties.seedPassword()), UUID.fromString("10000000-0000-0000-0000-000000000001"));
        staff.saveDemoAccount("peri", encoder.encode(properties.seedPassword()), UUID.fromString("10000000-0000-0000-0000-000000000002"));
    }
}
