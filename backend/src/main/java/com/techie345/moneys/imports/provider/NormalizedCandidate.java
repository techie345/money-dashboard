package com.techie345.moneys.imports.provider;
import com.techie345.moneys.financial.CategorySource;
import com.techie345.moneys.financial.TransactionKind;
import com.techie345.moneys.financial.ReviewStatus;
import com.techie345.moneys.financial.TransactionDirection;
import com.techie345.moneys.financial.TransferType;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.UUID;
public record NormalizedCandidate(int rowNumber, UUID accountId, LocalDate date, String merchant, String description, long amountCents, TransactionKind kind, String category, CategorySource categorySource, String sourceId, TransactionDirection direction, TransferType transferType, ReviewStatus reviewStatus, String action, String symbol, BigDecimal shares, Long priceCents) {
    public NormalizedCandidate(int rowNumber, UUID accountId, LocalDate date, String merchant, String description, long amountCents, TransactionKind kind, String category, CategorySource categorySource, String sourceId) {
        this(rowNumber, accountId, date, merchant, description, amountCents, kind, category, categorySource, sourceId, null, null, ReviewStatus.NEEDS_REVIEW, null, null, null, null);
    }
    public NormalizedCandidate withCategory(String value, CategorySource source) { return new NormalizedCandidate(rowNumber, accountId, date, merchant, description, amountCents, kind, value, source, sourceId, direction, transferType, reviewStatus, action, symbol, shares, priceCents); }
}
