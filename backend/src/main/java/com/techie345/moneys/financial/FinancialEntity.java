package com.techie345.moneys.financial;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;

@MappedSuperclass
public abstract class FinancialEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) protected UUID id;
    @Column(name = "owner_id", nullable = false) protected UUID ownerId;
    @Version @Column(nullable = false) protected long version;
    @Column(name = "created_at", nullable = false) protected Instant createdAt;
    @Column(name = "updated_at", nullable = false) protected Instant updatedAt;
    @PrePersist void created() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void updated() { updatedAt = Instant.now(); }
    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public long getVersion() { return version; }
}
