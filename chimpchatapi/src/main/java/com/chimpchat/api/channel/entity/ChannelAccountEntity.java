package com.chimpchat.api.channel.entity;

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
@Table(name = "channel_accounts", uniqueConstraints = @UniqueConstraint(
        name = "uq_channel_account_connection_external",
        columnNames = { "connection_id", "external_account_id" }))
public class ChannelAccountEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "connection_id", nullable = false)
    private ChannelConnectionEntity connection;

    @Column(name = "external_account_id", nullable = false, length = 255)
    private String externalAccountId;

    @Column(nullable = false, length = 255)
    private String username;

    protected ChannelAccountEntity() {}

    public ChannelAccountEntity(String externalAccountId, String username) {
        id = UUID.randomUUID();
        this.externalAccountId = externalAccountId;
        this.username = username;
    }

    void attachTo(ChannelConnectionEntity connection) {
        this.connection = connection;
    }

    public String getExternalAccountId() {
        return externalAccountId;
    }

    public String getUsername() {
        return username;
    }
}
