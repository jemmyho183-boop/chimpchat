package com.chimpchat.api.auth.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.chimpchat.api.auth.entity.EmailLoginTokenEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface EmailLoginTokenRepository extends JpaRepository<EmailLoginTokenEntity, UUID> {
    @Transactional
    void deleteByEmailAndConsumedAtIsNull(String email);

    @Transactional
    void deleteByTokenHash(String tokenHash);

    @Transactional
    void deleteByExpiresAtBeforeOrConsumedAtBefore(Instant expiryCutoff, Instant consumedCutoff);

    long countByEmailAndConsumedAtIsNull(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select token from EmailLoginTokenEntity token
            where token.tokenHash = :tokenHash
              and token.consumedAt is null
              and token.expiresAt > :now
            """)
    Optional<EmailLoginTokenEntity> findActiveForUpdate(
            @Param("tokenHash") String tokenHash,
            @Param("now") Instant now);
}
