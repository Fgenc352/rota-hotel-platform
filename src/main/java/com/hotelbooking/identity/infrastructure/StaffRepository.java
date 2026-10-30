package com.hotelbooking.identity.infrastructure;

import com.hotelbooking.hotel.application.port.HotelCatalog;
import com.hotelbooking.identity.application.model.StaffCredentials;
import com.hotelbooking.identity.application.model.StaffIdentity;
import com.hotelbooking.identity.application.port.StaffAccounts;
import com.hotelbooking.identity.infrastructure.entity.StaffUserEntity;
import com.hotelbooking.identity.infrastructure.jpa.StaffJpaRepository;
import com.hotelbooking.identity.infrastructure.mapper.StaffEntityMapper;
import com.hotelbooking.shared.exception.BusinessException;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional
public class StaffRepository implements StaffAccounts {
    private final EntityManager entityManager;
    private final StaffJpaRepository staff;
    private final HotelCatalog hotels;
    private final StaffEntityMapper mapper;

    public StaffIdentity identity(String username) {
        var staff = find(username);
        return mapper.toIdentity(staff, hotels.byId(staff.getHotelId()));
    }
    public UUID hotel(String username) {
        return find(username).getHotelId();
    }
    public Optional<StaffCredentials> credentials(String username) {
        return staff.findById(username).map(mapper::toCredentials);
    }
    public void saveDemoAccount(String username, String passwordHash, UUID hotel) {
        entityManager.flush();
        int changed = entityManager.createNativeQuery("""
            INSERT INTO staff_user(username,password_hash,hotel_id) VALUES (:user,:hash,:hotel)
            ON CONFLICT (username) DO UPDATE SET password_hash=excluded.password_hash
            WHERE staff_user.hotel_id=excluded.hotel_id
            """).setParameter("user",
            username).setParameter("hash", passwordHash).setParameter("hotel", hotel).executeUpdate();
        if (changed != 1) {
            throw new IllegalStateException("Demo account belongs to another hotel: " + username);
        }
        entityManager.refresh(find(username));
    }
    private StaffUserEntity find(String username) {
        return staff.findById(username).orElseThrow(BusinessException::missing);
    }
}
