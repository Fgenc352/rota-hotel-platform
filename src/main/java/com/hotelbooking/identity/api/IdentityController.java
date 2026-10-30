package com.hotelbooking.identity.api;

import com.hotelbooking.identity.application.model.StaffIdentity;
import com.hotelbooking.identity.application.query.GetStaffIdentityHandler;
import jakarta.servlet.http.HttpServletResponse;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class IdentityController {
    private final GetStaffIdentityHandler staffIdentity;

    @GetMapping("/api/auth/me")
    StaffIdentity me(Principal user, CsrfToken csrf, HttpServletResponse response) {
        response.setHeader("X-CSRF-TOKEN", csrf.getToken());
        return staffIdentity.handle(new GetStaffIdentityHandler.Input(user.getName()));
    }
}
