package com.chimpchat.api.channel.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.chimpchat.api.workspace.entity.WorkspaceEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "channel_connections", uniqueConstraints = @UniqueConstraint(
        name = "uq_channel_connection_workspace_provider",
        columnNames = { "workspace_id", "provider" }))
public class ChannelConnectionEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private WorkspaceEntity workspace;

    @Column(nullable = false, length = 32)
    private String provider;

    @Column(name = "external_business_id", nullable = false, length = 255)
    private String externalBusinessId;

    @Column(name = "encrypted_access_token", nullable = false, columnDefinition = "text")
    private String encryptedAccessToken;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "connection", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("username ASC")
    private List<ChannelAccountEntity> accounts = new ArrayList<>();

    protected ChannelConnectionEntity() {}

    public ChannelConnectionEntity(WorkspaceEntity workspace, String provider, String businessId, String encryptedToken) {
        this.id = UUID.randomUUID();
        this.workspace = workspace;
        this.provider = provider;
        this.externalBusinessId = businessId;
        this.encryptedAccessToken = encryptedToken;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void updateCredentials(String businessId, String encryptedToken) {
        externalBusinessId = businessId;
        encryptedAccessToken = encryptedToken;
        updatedAt = Instant.now();
    }

    public void replaceAccounts(List<ChannelAccountEntity> nextAccounts) {
        accounts.clear();
        for (ChannelAccountEntity account : nextAccounts) {
            account.attachTo(this);
            accounts.add(account);
        }
    }

    public UUID getId() {
        return id;
    }

    public String getProvider() {
        return provider;
    }

    public String getEncryptedAccessToken() {
        return encryptedAccessToken;
    }

    public List<ChannelAccountEntity> getAccounts() {
        return List.copyOf(accounts);
    }
}
