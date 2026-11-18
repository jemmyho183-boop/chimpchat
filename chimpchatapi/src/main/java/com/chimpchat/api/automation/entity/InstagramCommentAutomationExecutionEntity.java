package com.chimpchat.api.automation.entity;

import java.time.Instant;
import java.util.UUID;

import com.chimpchat.api.channel.entity.InstagramCommentEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "instagram_comment_automation_executions", uniqueConstraints = @UniqueConstraint(
        name = "uq_instagram_automation_execution_comment_flow",
        columnNames = { "comment_id", "automation_id" }))
public class InstagramCommentAutomationExecutionEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    private InstagramCommentEntity comment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "automation_id", nullable = false)
    private InstagramCommentAutomationEntity automation;

    @Column(name = "public_reply_status", nullable = false, length = 16)
    private String publicReplyStatus = "PENDING";

    @Column(name = "direct_message_status", nullable = false, length = 16)
    private String directMessageStatus = "PENDING";

    @Column(name = "direct_message_id", length = 255)
    private String directMessageId;

    @Column(name = "link_button_status", nullable = false, length = 16)
    private String linkButtonStatus = "NOT_APPLICABLE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected InstagramCommentAutomationExecutionEntity() {}

    public InstagramCommentAutomationExecutionEntity(
            InstagramCommentEntity comment, InstagramCommentAutomationEntity automation) {
        this.id = UUID.randomUUID();
        this.comment = comment;
        this.automation = automation;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public InstagramCommentEntity getComment() {
        return comment;
    }

    public InstagramCommentAutomationEntity getAutomation() {
        return automation;
    }

    public void markPublicReplySent() {
        publicReplyStatus = "SENT";
    }

    public void markPublicReplyFailed(boolean outcomeUnknown) {
        publicReplyStatus = outcomeUnknown ? "UNKNOWN" : "FAILED";
    }

    public void markDirectMessageSent(String messageId, boolean hasLinkButton) {
        directMessageStatus = "SENT";
        directMessageId = messageId;
        linkButtonStatus = hasLinkButton ? "SENT" : "NOT_APPLICABLE";
    }

    public void markDirectMessageFailed(boolean outcomeUnknown) {
        directMessageStatus = outcomeUnknown ? "UNKNOWN" : "FAILED";
    }

    public void markLinkButtonProcessing() {
        linkButtonStatus = "PROCESSING";
    }

    public void markLinkButtonSent() {
        linkButtonStatus = "SENT";
    }

    public void markLinkButtonFailed(boolean outcomeUnknown) {
        linkButtonStatus = outcomeUnknown ? "UNKNOWN" : "FAILED";
    }
}
