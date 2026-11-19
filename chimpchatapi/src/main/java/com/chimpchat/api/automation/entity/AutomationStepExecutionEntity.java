package com.chimpchat.api.automation.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "automation_step_executions", uniqueConstraints = @UniqueConstraint(
        name = "uq_automation_step_execution", columnNames = { "execution_id", "step_id" }))
public class AutomationStepExecutionEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "execution_id", nullable = false)
    private InstagramCommentAutomationExecutionEntity execution;

    @Column(name = "step_id", nullable = false, length = 100)
    private String stepId;

    @Column(name = "step_type", nullable = false, length = 100)
    private String stepType;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "provider_result_id", length = 255)
    private String providerResultId;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected AutomationStepExecutionEntity() {}

    public AutomationStepExecutionEntity(
            InstagramCommentAutomationExecutionEntity execution,
            String stepId,
            String stepType) {
        this.id = UUID.randomUUID();
        this.execution = execution;
        this.stepId = stepId;
        this.stepType = stepType;
        this.status = "PROCESSING";
        this.startedAt = Instant.now();
    }

    public void markSent(String resultId) {
        status = "SENT";
        providerResultId = resultId;
        completedAt = Instant.now();
    }

    public void markFailed(boolean outcomeUnknown, String message) {
        status = outcomeUnknown ? "UNKNOWN" : "FAILED";
        errorMessage = message == null ? null : message.substring(0, Math.min(message.length(), 1000));
        completedAt = Instant.now();
    }
}
