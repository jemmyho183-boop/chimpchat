package com.chimpchat.api.automation.entity;

import java.time.Instant;
import java.util.UUID;

import com.chimpchat.api.channel.entity.ChannelConnectionEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "instagram_comment_automations", uniqueConstraints = @UniqueConstraint(
        name = "uq_instagram_automation_connection_account_media",
        columnNames = { "connection_id", "instagram_account_id", "external_media_id" }))
public class InstagramCommentAutomationEntity {
    public static final String ANY_COMMENT = "ANY_COMMENT";
    public static final String KEYWORD = "KEYWORD";

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "connection_id", nullable = false)
    private ChannelConnectionEntity connection;

    @Column(name = "instagram_account_id", nullable = false, length = 255)
    private String instagramAccountId;

    @Column(name = "external_media_id", nullable = false, length = 255)
    private String externalMediaId;

    @Column(name = "media_label", nullable = false, length = 500)
    private String mediaLabel;

    @Column(name = "match_type", nullable = false, length = 16)
    private String matchType;

    @Column(length = 100)
    private String keyword;

    @Column(name = "public_reply", nullable = false, columnDefinition = "text")
    private String publicReply;

    @Column(name = "direct_message", nullable = false, columnDefinition = "text")
    private String directMessage;

    @Column(name = "direct_message_link_text", length = 100)
    private String directMessageLinkText;

    @Column(name = "direct_message_link_url", length = 900)
    private String directMessageLinkUrl;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InstagramCommentAutomationEntity() {}

    public InstagramCommentAutomationEntity(
            ChannelConnectionEntity connection,
            String instagramAccountId,
            String externalMediaId,
            String mediaLabel,
            String matchType,
            String keyword,
            String publicReply,
            String directMessage,
            String directMessageLinkText,
            String directMessageLinkUrl) {
        this.id = UUID.randomUUID();
        this.connection = connection;
        this.instagramAccountId = instagramAccountId;
        this.externalMediaId = externalMediaId;
        update(
                mediaLabel, matchType, keyword, publicReply,
                directMessage, directMessageLinkText, directMessageLinkUrl, true);
        this.createdAt = Instant.now();
    }

    public void update(
            String mediaLabel,
            String matchType,
            String keyword,
            String publicReply,
            String directMessage,
            String directMessageLinkText,
            String directMessageLinkUrl,
            boolean enabled) {
        this.mediaLabel = mediaLabel;
        this.matchType = matchType;
        this.keyword = keyword;
        this.publicReply = publicReply;
        this.directMessage = directMessage;
        this.directMessageLinkText = directMessageLinkText;
        this.directMessageLinkUrl = directMessageLinkUrl;
        this.enabled = enabled;
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

    public String getExternalMediaId() {
        return externalMediaId;
    }

    public String getMediaLabel() {
        return mediaLabel;
    }

    public String getMatchType() {
        return matchType;
    }

    public String getKeyword() {
        return keyword;
    }

    public String getPublicReply() {
        return publicReply;
    }

    public String getDirectMessage() {
        return directMessage;
    }

    public String getDirectMessageLinkText() {
        return directMessageLinkText;
    }

    public String getDirectMessageLinkUrl() {
        return directMessageLinkUrl;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
