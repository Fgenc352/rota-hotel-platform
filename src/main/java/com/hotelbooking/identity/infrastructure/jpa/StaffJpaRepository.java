package com.hotelbooking.identity.infrastructure.jpa;

import com.hotelbooking.identity.infrastructure.entity.StaffUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffJpaRepository extends JpaRepository<StaffUserEntity, String> {

}
