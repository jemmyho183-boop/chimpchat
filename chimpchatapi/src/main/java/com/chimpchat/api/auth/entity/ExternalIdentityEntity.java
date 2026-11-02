package com.chimpchat.api.auth.entity;

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
@Table(name = "external_identities", uniqueConstraints = {
        @UniqueConstraint(name = "uq_external_identity_provider_subject", columnNames = { "provider", "subject" }),
        @UniqueConstraint(name = "uq_external_identity_user_provider", columnNames = { "user_id", "provider" })
})
public class ExternalIdentityEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false, length = 32)
    private String provider;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ExternalIdentityEntity() {}

    public ExternalIdentityEntity(UserEntity user, String provider, String subject) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.provider = provider;
        this.subject = subject;
        this.createdAt = Instant.now();
    }

    public UserEntity getUser() {
        return user;
    }
}
