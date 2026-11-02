package com.chimpchat.api.auth.repository;

import java.util.Optional;
import java.util.UUID;

import com.chimpchat.api.auth.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmail(String email);
}
