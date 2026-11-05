package com.chimpchat.api.workspace.mapper;

import com.chimpchat.api.workspace.dto.WorkspaceResponseDto;
import com.chimpchat.api.workspace.entity.WorkspaceEntity;
import org.mapstruct.Mapper;

@Mapper
public interface WorkspaceMapper {
    WorkspaceResponseDto toResponse(WorkspaceEntity workspace);
}
