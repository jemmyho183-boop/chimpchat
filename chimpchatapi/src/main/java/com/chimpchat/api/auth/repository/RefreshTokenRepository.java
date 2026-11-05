package com.chimpchat.api.auth.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.chimpchat.api.auth.entity.RefreshTokenEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select token from RefreshTokenEntity token
            join fetch token.user
            where token.tokenHash = :tokenHash
              and token.revokedAt is null
              and token.expiresAt > :now
            """)
    Optional<RefreshTokenEntity> findActiveForUpdate(
            @Param("tokenHash") String tokenHash,
            @Param("now") Instant now);
}
