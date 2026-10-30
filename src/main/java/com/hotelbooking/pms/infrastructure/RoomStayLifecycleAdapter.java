package com.hotelbooking.pms.infrastructure;

import com.hotelbooking.contracts.pms.RoomStayLifecycle;
import com.hotelbooking.pms.application.command.ApplyStayTransitionHandler;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoomStayLifecycleAdapter implements RoomStayLifecycle {
    private final ApplyStayTransitionHandler transitions;

    public void transition(UUID hotel, UUID reservation, String status) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = auth != null && auth.isAuthenticated() ? auth.getName() : "system";
        transitions.handle(new ApplyStayTransitionHandler.Input(hotel, reservation, status, actor));
    }
}
