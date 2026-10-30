package com.hotelbooking.pms.application.query;

import com.hotelbooking.pms.application.model.RoomAuditPage;
import com.hotelbooking.pms.application.port.RoomBoardQueries;
import com.hotelbooking.shared.exception.BusinessException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetRoomHistoryHandler {
    private final RoomBoardQueries queries;

    public RoomAuditPage handle(UUID hotel, int page, int size) {
        if (page < 0 || page > 1_000_000 || size < 1 || size > 100) {
            throw BusinessException.invalid("Sayfa 0 veya daha büyük, sayfa boyutu 1–100 olmalıdır.");
        }
        return queries.historyPage(hotel, page, size);
    }
}
