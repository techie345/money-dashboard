package com.techie345.moneys.financial.account;

import java.time.Instant;
import java.util.UUID;

import com.techie345.moneys.financial.AccountKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "account")
public class AccountEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "owner_id", nullable = false) private UUID ownerId;
    @Column(nullable = false, length = 200) private String institution;
    @Column(nullable = false, length = 200) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private AccountKind kind;
    @Column(name = "balance_cents", nullable = false) private long balanceCents;
    @Version @Column(nullable = false) private long version;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "last_imported_at") private Instant lastImportedAt;
    @Column(length = 100) private String source;
    @Column(name = "source_file", length = 500) private String sourceFile;
    @Column(name = "imported_at") private Instant importedAt;

    protected AccountEntity() { }

    public AccountEntity(UUID ownerId, String institution, String name, AccountKind kind, long balanceCents) {
        this.ownerId = ownerId;
        this.institution = institution;
        this.name = name;
        this.kind = kind;
        this.balanceCents = balanceCents;
    }

    @jakarta.persistence.PrePersist
    void created() { createdAt = updatedAt = Instant.now(); }

    @jakarta.persistence.PreUpdate
    void updated() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public String getInstitution() { return institution; }
    public String getName() { return name; }
    public AccountKind getKind() { return kind; }
    public long getBalanceCents() { return balanceCents; }
    public long getVersion() { return version; }
    public Instant getLastImportedAt(){return lastImportedAt;} public String getSource(){return source;} public String getSourceFile(){return sourceFile;} public Instant getImportedAt(){return importedAt;}

    public void update(String institution, String name, AccountKind kind, long balanceCents) {
        this.institution = institution; this.name = name; this.kind = kind; this.balanceCents = balanceCents;
    }
    public void updateMetadata(Instant lastImportedAt, String source, String sourceFile, Instant importedAt) {
        this.lastImportedAt=lastImportedAt; this.source=source; this.sourceFile=sourceFile; this.importedAt=importedAt;
    }
}
