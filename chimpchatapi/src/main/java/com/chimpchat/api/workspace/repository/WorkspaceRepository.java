package com.chimpchat.api.workspace.repository;

import java.util.Optional;
import java.util.UUID;

import com.chimpchat.api.workspace.entity.WorkspaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<WorkspaceEntity, UUID> {
    Optional<WorkspaceEntity> findByOwnerId(UUID ownerId);
}
