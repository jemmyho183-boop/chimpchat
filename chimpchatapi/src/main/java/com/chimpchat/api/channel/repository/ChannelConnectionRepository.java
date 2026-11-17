package com.chimpchat.api.channel.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.chimpchat.api.channel.entity.ChannelConnectionEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChannelConnectionRepository extends JpaRepository<ChannelConnectionEntity, UUID> {
    @EntityGraph(attributePaths = "accounts")
    Optional<ChannelConnectionEntity> findByWorkspaceIdAndProvider(UUID workspaceId, String provider);

    boolean existsByWorkspaceIdAndProvider(UUID workspaceId, String provider);

    long deleteByIdAndWorkspaceIdAndProvider(UUID id, UUID workspaceId, String provider);

    @Query("""
            select distinct connection from ChannelConnectionEntity connection
            join fetch connection.accounts account
            where connection.provider = :provider
                and account.externalAccountId = :externalAccountId
            """)
    List<ChannelConnectionEntity> findAllWithAccountByProviderAndExternalAccountId(
            @Param("provider") String provider,
            @Param("externalAccountId") String externalAccountId);
}
