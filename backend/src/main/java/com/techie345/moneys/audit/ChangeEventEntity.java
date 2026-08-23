package com.techie345.moneys.audit;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "change_event")
public class ChangeEventEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "owner_id", nullable = false) private UUID ownerId;
    @Column(name = "change_sequence", nullable = false)
    private long sequence;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private ChangeOperation operation;
    @Column(name = "resource_type", nullable = false, length = 64) private String resourceType;
    @Column(name = "resource_id", nullable = false) private UUID resourceId;
    @Column(name = "source_id", length = 300) private String sourceId;
    @Column(name = "import_id") private UUID importId;
    @Column(name = "changed_at", nullable = false) private Instant changedAt;
    @Column(nullable = false) private long version;
    @Column(nullable = false, columnDefinition = "text") private String payload;

    protected ChangeEventEntity() { }

    public ChangeEventEntity(UUID ownerId, long sequence, ChangeOperation operation, String resourceType, UUID resourceId,
                             String sourceId, UUID importId, long version, String payload) {
        this.ownerId = ownerId;
        this.sequence = sequence;
        this.operation = operation;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.sourceId = sourceId;
        this.importId = importId;
        this.version = version;
        this.payload = payload;
        this.changedAt = Instant.now();
    }

    public ChangeEventEntity(UUID ownerId, ChangeOperation operation, String resourceType, UUID resourceId,
                             String sourceId, UUID importId, long version, String payload) {
        this(ownerId, 0, operation, resourceType, resourceId, sourceId, importId, version, payload);
    }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public long getSequence() { return sequence; }
    public ChangeOperation getOperation() { return operation; }
    public String getResourceType() { return resourceType; }
    public UUID getResourceId() { return resourceId; }
    public String getSourceId() { return sourceId; }
    public UUID getImportId() { return importId; }
    public Instant getChangedAt() { return changedAt; }
    public long getVersion() { return version; }
    public String getPayload() { return payload; }
}
