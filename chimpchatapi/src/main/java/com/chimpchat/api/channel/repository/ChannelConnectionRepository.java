package com.chimpchat.api.channel.repository;

import java.util.Optional;
import java.util.UUID;

import com.chimpchat.api.channel.entity.ChannelConnectionEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelConnectionRepository extends JpaRepository<ChannelConnectionEntity, UUID> {
    @EntityGraph(attributePaths = "accounts")
    Optional<ChannelConnectionEntity> findByWorkspaceIdAndProvider(UUID workspaceId, String provider);

    boolean existsByWorkspaceIdAndProvider(UUID workspaceId, String provider);

    long deleteByIdAndWorkspaceIdAndProvider(UUID id, UUID workspaceId, String provider);
}
