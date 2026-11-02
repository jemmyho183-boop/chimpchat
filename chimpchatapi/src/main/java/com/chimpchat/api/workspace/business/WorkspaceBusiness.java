package com.chimpchat.api.workspace.business;

import java.util.UUID;

import com.chimpchat.api.auth.entity.UserEntity;
import com.chimpchat.api.workspace.entity.WorkspaceEntity;
import com.chimpchat.api.workspace.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceBusiness {
    private final WorkspaceRepository workspaceRepository;

    public WorkspaceBusiness(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    @Transactional
    public WorkspaceEntity getOrCreatePersonalWorkspace(UserEntity owner) {
        return workspaceRepository.findByOwnerId(owner.getId())
                .orElseGet(() -> workspaceRepository.save(new WorkspaceEntity(owner, "My workspace")));
    }

    @Transactional(readOnly = true)
    public WorkspaceEntity requirePersonalWorkspace(UUID ownerId) {
        return workspaceRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new IllegalStateException("The signed-in user has no personal workspace."));
    }
}
