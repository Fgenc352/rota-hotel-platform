package com.hotelbooking.unit;

import com.hotelbooking.guest.infrastructure.ActiveGuestLookup;
import com.hotelbooking.guest.infrastructure.GuestProfileMerger;
import com.hotelbooking.guest.infrastructure.GuestMatchSnapshotReader;
import com.hotelbooking.guest.infrastructure.jpa.GuestJpaRepository;
import com.hotelbooking.guest.infrastructure.mapper.GuestMatchAuditMapper;
import com.hotelbooking.hotel.application.port.HotelCatalog;
import com.hotelbooking.shared.exception.BusinessException;
import com.hotelbooking.shared.persistence.JpaQueries;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("unit")
class GuestProfileMergerTest {
    private final HotelCatalog hotels = mock(HotelCatalog.class);
    private final GuestJpaRepository guests = mock(GuestJpaRepository.class);
    private final JpaQueries queries = mock(JpaQueries.class);
    private final GuestProfileMerger merger = new GuestProfileMerger(hotels, new ActiveGuestLookup(guests), queries,
        mock(EntityManager.class), mock(GuestMatchSnapshotReader.class), new GuestMatchAuditMapper());

    @Test
    void automaticConsolidationCannotMoveAForeignHotelProfile() {
        UUID hotel = UUID.randomUUID(), source = UUID.randomUUID(), target = UUID.randomUUID();
        when(queries.tuples(anyString(), anyMap())).thenReturn(List.of());
        when(guests.findByIdAndHotelId(source, hotel)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> merger.merge(hotel, source, target)).isInstanceOf(BusinessException.class);
        verify(guests).findByIdAndHotelId(source, hotel);
        verify(queries, never()).execute(anyString(), anyMap());
    }

    @Test
    void aProfileCannotBeConsolidatedIntoItself() {
        UUID hotel = UUID.randomUUID(), guest = UUID.randomUUID();
        assertThatThrownBy(() -> merger.merge(hotel, guest, guest)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(guests, queries);
    }
}
