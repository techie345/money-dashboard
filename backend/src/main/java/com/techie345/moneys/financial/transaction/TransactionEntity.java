package com.techie345.moneys.financial.transaction;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.techie345.moneys.financial.*;
import jakarta.persistence.*;

@Entity
@Table(name = "financial_transaction")
public class TransactionEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "owner_id", nullable = false) private UUID ownerId;
    @Column(name = "account_id", nullable = false) private UUID accountId;
    @Column(name = "transaction_date", nullable = false) private LocalDate date;
    @Column(nullable = false, length = 300) private String merchant;
    @Column(nullable = false, length = 1000) private String description;
    @Column(name = "amount_cents", nullable = false) private long amountCents;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private TransactionKind kind;
    @Column(nullable = false, length = 64) private String category;
    @Enumerated(EnumType.STRING) @Column(name = "category_source", length = 16) private CategorySource categorySource;
    @Column(nullable = false, length = 100) private String source;
    @Column(name = "source_file", nullable = false, length = 500) private String sourceFile;
    @Column(name = "source_id", length = 300) private String sourceId;
    @Enumerated(EnumType.STRING) @Column(name = "review_status", length = 16) private ReviewStatus reviewStatus;
    @Enumerated(EnumType.STRING) @Column(length = 16) private TransactionDirection direction;
    @Enumerated(EnumType.STRING) @Column(name = "transfer_type", length = 40) private TransferType transferType;
    @Column(name = "imported_at") private Instant importedAt;
    @Column(length = 200) private String action;
    @Column(length = 32) private String symbol;
    @Column(precision = 24, scale = 8) private java.math.BigDecimal shares;
    @Column(name = "price_cents") private Long priceCents;
    @Version @Column(nullable = false) private long version;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected TransactionEntity() { }

    public TransactionEntity(UUID ownerId, UUID accountId, LocalDate date, String merchant, String description,
                             long amountCents, TransactionKind kind, String category, CategorySource categorySource,
                             String source, String sourceFile, String sourceId) {
         if (ownerId == null || accountId == null || date == null || merchant == null || merchant.isBlank() || description == null || description.isBlank() || kind == null || category == null || category.isBlank() || source == null || source.isBlank() || sourceFile == null || sourceFile.isBlank() || amountCents <= 0) throw new IllegalArgumentException("invalid transaction");
        this.ownerId = ownerId; this.accountId = accountId; this.date = date; this.merchant = merchant;
        this.description = description; this.amountCents = amountCents; this.kind = kind; this.category = category;
        this.categorySource = categorySource; this.source = source; this.sourceFile = sourceFile; this.sourceId = sourceId;
    }

    @PrePersist void created() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void updated() { updatedAt = Instant.now(); }
    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public UUID getAccountId() { return accountId; }
    public LocalDate getDate() { return date; }
    public String getMerchant() { return merchant; }
    public String getDescription() { return description; }
    public long getAmountCents() { return amountCents; }
    public TransactionKind getKind() { return kind; }
    public String getCategory() { return category; }
    public CategorySource getCategorySource() { return categorySource; }
    public String getSource() { return source; }
    public String getSourceFile() { return sourceFile; }
    public String getSourceId() { return sourceId; }
    public long getVersion() { return version; }
    public ReviewStatus getReviewStatus() { return reviewStatus; } public TransactionDirection getDirection() { return direction; }
    public TransferType getTransferType() { return transferType; } public Instant getImportedAt() { return importedAt; }
    public String getAction() { return action; } public String getSymbol() { return symbol; }
    public java.math.BigDecimal getShares() { return shares; } public Long getPriceCents() { return priceCents; }
    public void updateMetadata(ReviewStatus status, TransactionDirection direction, TransferType transferType, Instant importedAt,
                               String action, String symbol, java.math.BigDecimal shares, Long priceCents) {
        this.reviewStatus=status; this.direction=direction; this.transferType=transferType; this.importedAt=importedAt;
        this.action=action; this.symbol=symbol; this.shares=shares; this.priceCents=priceCents;
    }
    public void update(LocalDate date, String merchant, String description, long amountCents, TransactionKind kind,
                       String category, UUID accountId, CategorySource categorySource, String source, String sourceFile,
                       String sourceId) {
        if (amountCents <= 0) throw new IllegalArgumentException("amountCents must be positive");
        this.date=date; this.merchant=merchant; this.description=description; this.amountCents=amountCents;
        this.kind=kind; this.category=category; this.accountId=accountId; this.categorySource=categorySource;
        this.source=source; this.sourceFile=sourceFile; this.sourceId=sourceId;
    }
}
