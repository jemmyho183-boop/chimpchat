package com.chimpchat.api.auth.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "email_login_tokens")
public class EmailLoginTokenEntity {
    @Id
    private UUID id;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "request_ip", length = 64)
    private String requestIp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected EmailLoginTokenEntity() {}

    public EmailLoginTokenEntity(String email, String tokenHash, Instant expiresAt, String requestIp) {
        this.id = UUID.randomUUID();
        this.email = email;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.requestIp = requestIp;
        this.createdAt = Instant.now();
    }

    public String getEmail() {
        return email;
    }

    public void consume(Instant now) {
        consumedAt = now;
    }
}
