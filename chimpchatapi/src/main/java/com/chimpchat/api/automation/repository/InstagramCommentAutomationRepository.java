package com.chimpchat.api.automation.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.chimpchat.api.automation.entity.InstagramCommentAutomationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InstagramCommentAutomationRepository
        extends JpaRepository<InstagramCommentAutomationEntity, UUID> {
    boolean existsByConnectionIdAndInstagramAccountIdAndExternalMediaId(
            UUID connectionId, String accountId, String mediaId);

    @Query("""
            select automation from InstagramCommentAutomationEntity automation
            join fetch automation.connection connection
            where connection.workspace.id = :workspaceId
                and (:accountId is null or automation.instagramAccountId = :accountId)
            order by automation.createdAt desc
            """)
    List<InstagramCommentAutomationEntity> findAllForWorkspace(
            @Param("workspaceId") UUID workspaceId,
            @Param("accountId") String accountId);

    @Query("""
            select automation from InstagramCommentAutomationEntity automation
            where automation.connection.id = :connectionId
                and automation.instagramAccountId = :accountId
                and automation.externalMediaId = :mediaId
                and automation.enabled = true
            """)
    List<InstagramCommentAutomationEntity> findEnabledForMedia(
            @Param("connectionId") UUID connectionId,
            @Param("accountId") String accountId,
            @Param("mediaId") String mediaId);

    @Query("""
            select automation from InstagramCommentAutomationEntity automation
            join fetch automation.connection
            where automation.id = :automationId
                and automation.connection.workspace.id = :workspaceId
            """)
    Optional<InstagramCommentAutomationEntity> findByIdForWorkspace(
            @Param("automationId") UUID automationId,
            @Param("workspaceId") UUID workspaceId);
}
