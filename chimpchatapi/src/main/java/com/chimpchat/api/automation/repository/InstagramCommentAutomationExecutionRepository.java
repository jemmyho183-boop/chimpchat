package com.chimpchat.api.automation.repository;

import java.util.UUID;

import com.chimpchat.api.automation.entity.InstagramCommentAutomationExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstagramCommentAutomationExecutionRepository
        extends JpaRepository<InstagramCommentAutomationExecutionEntity, UUID> {
    boolean existsByCommentIdAndAutomationId(UUID commentId, UUID automationId);
}
