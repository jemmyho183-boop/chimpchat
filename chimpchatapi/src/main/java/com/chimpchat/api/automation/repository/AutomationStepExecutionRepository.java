package com.chimpchat.api.automation.repository;

import java.util.UUID;

import com.chimpchat.api.automation.entity.AutomationStepExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AutomationStepExecutionRepository
        extends JpaRepository<AutomationStepExecutionEntity, UUID> {}
