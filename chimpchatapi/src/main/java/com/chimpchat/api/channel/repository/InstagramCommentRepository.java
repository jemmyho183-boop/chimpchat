package com.chimpchat.api.channel.repository;

import java.util.Optional;
import java.util.UUID;

import com.chimpchat.api.channel.entity.InstagramCommentEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InstagramCommentRepository extends JpaRepository<InstagramCommentEntity, UUID> {
    Optional<InstagramCommentEntity> findByConnectionIdAndInstagramAccountIdAndExternalCommentId(
            UUID connectionId,
            String instagramAccountId,
            String externalCommentId);

    @Query("""
            select comment from InstagramCommentEntity comment
            join fetch comment.connection
            where comment.id = :commentId
            """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<InstagramCommentEntity> findWithConnectionById(@Param("commentId") UUID commentId);
}
