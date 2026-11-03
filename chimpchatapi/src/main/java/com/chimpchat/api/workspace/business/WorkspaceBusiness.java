package com.chimpchat.api.workspace.business;

import java.util.UUID;

import com.chimpchat.api.auth.entity.UserEntity;
import com.chimpchat.api.workspace.entity.WorkspaceEntity;
import com.chimpchat.api.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkspaceBusiness {
    private final WorkspaceRepository workspaceRepository;

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
