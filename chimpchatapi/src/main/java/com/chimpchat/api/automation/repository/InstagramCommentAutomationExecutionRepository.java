package com.chimpchat.api.automation.repository;

import java.util.List;
import java.util.UUID;

import com.chimpchat.api.automation.entity.InstagramCommentAutomationExecutionEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InstagramCommentAutomationExecutionRepository
        extends JpaRepository<InstagramCommentAutomationExecutionEntity, UUID> {
    boolean existsByCommentIdAndAutomationId(UUID commentId, UUID automationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select execution from InstagramCommentAutomationExecutionEntity execution
            join fetch execution.comment comment
            join fetch execution.automation automation
            where comment.instagramAccountId = :accountId
                and comment.commenterId = :commenterId
                and execution.directMessageStatus = 'SENT'
                and execution.linkButtonStatus = 'PENDING'
                and automation.directMessageLinkUrl is not null
                and (:replyToMessageId is null or execution.directMessageId = :replyToMessageId)
            order by execution.createdAt desc
            """)
    List<InstagramCommentAutomationExecutionEntity> findPendingLinkButtonReply(
            @Param("accountId") String accountId,
            @Param("commenterId") String commenterId,
            @Param("replyToMessageId") String replyToMessageId,
            Pageable pageable);
}
