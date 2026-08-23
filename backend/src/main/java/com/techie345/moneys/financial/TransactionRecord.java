package com.techie345.moneys.financial;

import java.time.LocalDate;
import java.util.UUID;

public record TransactionRecord(
        UUID id,
        LocalDate date,
        String merchant,
        String description,
        long amountCents,
        TransactionKind kind,
        String category,
        UUID accountId,
        CategorySource categorySource,
        ReviewStatus reviewStatus
) {
    public TransactionRecord(UUID id, LocalDate date, String merchant, String description, long amountCents,
                             TransactionKind kind, String category, UUID accountId, CategorySource categorySource) {
        this(id, date, merchant, description, amountCents, kind, category, accountId, categorySource, ReviewStatus.NEEDS_REVIEW);
    }
    public TransactionRecord {
        if (amountCents <= 0) throw new IllegalArgumentException("amountCents must be positive");
        if (date == null || merchant == null || description == null || kind == null || category == null || accountId == null || reviewStatus == null) {
            throw new IllegalArgumentException("transaction fields must not be null");
        }
    }

    public static TransactionRecord create(LocalDate date, String merchant, String description,
                                           long amountCents, TransactionKind kind, String category,
                                           UUID accountId) {
        return create(date, merchant, description, amountCents, kind, category, accountId, ReviewStatus.NEEDS_REVIEW);
    }

    public static TransactionRecord create(LocalDate date, String merchant, String description,
                                           long amountCents, TransactionKind kind, String category,
                                           UUID accountId, ReviewStatus reviewStatus) {
        return new TransactionRecord(null, date, merchant, description, amountCents, kind, category,
                accountId, CategorySource.IMPORTED, reviewStatus);
    }

    public TransactionRecord withMerchant(String value) {
        return new TransactionRecord(id, date, value, description, amountCents, kind, category, accountId, categorySource, reviewStatus);
    }

    public TransactionRecord withCategorySource(CategorySource value) {
        return new TransactionRecord(id, date, merchant, description, amountCents, kind, category, accountId, value, reviewStatus);
    }

    public TransactionRecord withCategory(String value, CategorySource source) {
        return new TransactionRecord(id, date, merchant, description, amountCents, kind, value, accountId, source, reviewStatus);
    }
}
