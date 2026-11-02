package com.chimpchat.api.auth.repository;

import java.util.Optional;
import java.util.UUID;

import com.chimpchat.api.auth.entity.ExternalIdentityEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalIdentityRepository extends JpaRepository<ExternalIdentityEntity, UUID> {
    @EntityGraph(attributePaths = "user")
    Optional<ExternalIdentityEntity> findByProviderAndSubject(String provider, String subject);

    boolean existsByUserIdAndProvider(UUID userId, String provider);
}
