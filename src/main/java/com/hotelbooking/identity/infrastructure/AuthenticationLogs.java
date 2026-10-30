package com.hotelbooking.identity.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AuthenticationLogs {
    @EventListener
    public void authenticated(AuthenticationSuccessEvent event) {
        log.info("event=auth.login_succeeded");
    }

    @EventListener
    public void rejected(AbstractAuthenticationFailureEvent event) {
        log.warn("event=auth.login_failed reason={}", event.getException().getClass().getSimpleName());
    }

    @EventListener
    public void loggedOut(LogoutSuccessEvent event) {
        log.info("event=auth.logout_succeeded");
    }
}
