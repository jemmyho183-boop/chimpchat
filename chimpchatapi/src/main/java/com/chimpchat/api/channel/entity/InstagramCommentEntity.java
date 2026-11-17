package com.chimpchat.api.channel.entity;

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
@Table(name = "instagram_comments", uniqueConstraints = @UniqueConstraint(
        name = "uq_instagram_comment_connection_account_external",
        columnNames = { "connection_id", "instagram_account_id", "external_comment_id" }))
public class InstagramCommentEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "connection_id", nullable = false)
    private ChannelConnectionEntity connection;

    @Column(name = "instagram_account_id", nullable = false, length = 255)
    private String instagramAccountId;

    @Column(name = "instagram_account_username", nullable = false, length = 255)
    private String instagramAccountUsername;

    @Column(name = "external_comment_id", nullable = false, length = 255)
    private String externalCommentId;

    @Column(name = "external_media_id", length = 255)
    private String externalMediaId;

    @Column(name = "commenter_id", length = 255)
    private String commenterId;

    @Column(name = "commenter_username", length = 255)
    private String commenterUsername;

    @Column(name = "comment_text", columnDefinition = "text")
    private String commentText;

    @Column(name = "media_product_type", length = 64)
    private String mediaProductType;

    @Column(name = "comment_verb", length = 32)
    private String commentVerb;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InstagramCommentEntity() {}

    public InstagramCommentEntity(
            ChannelConnectionEntity connection,
            String instagramAccountId,
            String instagramAccountUsername,
            String externalCommentId,
            String externalMediaId,
            String commenterId,
            String commenterUsername,
            String commentText,
            String mediaProductType,
            String commentVerb) {
        this.id = UUID.randomUUID();
        this.connection = connection;
        this.instagramAccountId = instagramAccountId;
        this.instagramAccountUsername = instagramAccountUsername;
        this.externalCommentId = externalCommentId;
        updateFromWebhook(
                instagramAccountUsername,
                externalMediaId,
                commenterId,
                commenterUsername,
                commentText,
                mediaProductType,
                commentVerb);
        this.receivedAt = Instant.now();
        this.updatedAt = this.receivedAt;
    }

    public void updateFromWebhook(
            String instagramAccountUsername,
            String externalMediaId,
            String commenterId,
            String commenterUsername,
            String commentText,
            String mediaProductType,
            String commentVerb) {
        if (instagramAccountUsername != null) {
            this.instagramAccountUsername = instagramAccountUsername;
        }
        if (externalMediaId != null) {
            this.externalMediaId = externalMediaId;
        }
        if (commenterId != null) {
            this.commenterId = commenterId;
        }
        if (commenterUsername != null) {
            this.commenterUsername = commenterUsername;
        }
        if (commentText != null) {
            this.commentText = commentText;
        }
        if (mediaProductType != null) {
            this.mediaProductType = mediaProductType;
        }
        if (commentVerb != null) {
            this.commentVerb = commentVerb;
            if ("remove".equalsIgnoreCase(commentVerb)) {
                this.deleted = true;
            }
        }
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public ChannelConnectionEntity getConnection() {
        return connection;
    }

    public String getInstagramAccountId() {
        return instagramAccountId;
    }

    public String getInstagramAccountUsername() {
        return instagramAccountUsername;
    }

    public String getExternalCommentId() {
        return externalCommentId;
    }

    public String getExternalMediaId() {
        return externalMediaId;
    }

    public String getCommenterId() {
        return commenterId;
    }

    public String getCommenterUsername() {
        return commenterUsername;
    }

    public String getCommentText() {
        return commentText;
    }

    public String getMediaProductType() {
        return mediaProductType;
    }

    public String getCommentVerb() {
        return commentVerb;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

}
