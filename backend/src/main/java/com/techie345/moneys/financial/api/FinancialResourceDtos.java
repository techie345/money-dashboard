package com.techie345.moneys.financial.api;

import java.util.UUID;

import com.techie345.moneys.financial.AccountKind;
import com.techie345.moneys.financial.CategorySource;
import com.techie345.moneys.financial.TransactionKind;
import java.time.LocalDate;
import java.time.Instant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class FinancialResourceDtos {
    private FinancialResourceDtos() { }
    public record ApiError(String code, String message, java.util.Map<String, String> fields) { }

    public record AccountRequest(
            @NotBlank String institution,
            @NotBlank String name,
            @NotNull AccountKind kind,
            long balanceCents, Instant lastImportedAt, String source, String sourceFile, Instant importedAt) { }

    public record AccountResponse(UUID id, UUID ownerId, String institution, String name,
                                  AccountKind kind, long balanceCents, Instant lastImportedAt, String source, String sourceFile, Instant importedAt, long version) { }

    public record TransactionRequest(
            @NotNull LocalDate date,
            @NotBlank String merchant,
            @NotBlank String description,
            @jakarta.validation.constraints.Positive long amountCents,
            @NotNull TransactionKind kind,
            @jakarta.validation.constraints.Pattern(regexp = "^(housing|food|transportation|utilities|healthcare|shopping|entertainment|subscriptions|salary|other|transfer)$") String category,
            @NotNull UUID accountId,
            CategorySource categorySource,
            @NotBlank String source,
            @NotBlank String sourceFile,
            String sourceId, com.techie345.moneys.financial.ReviewStatus reviewStatus,
            com.techie345.moneys.financial.TransactionDirection direction,
            com.techie345.moneys.financial.TransferType transferType, Instant importedAt, String action,
            String symbol, java.math.BigDecimal shares, Long priceCents) { }

    public record TransactionResponse(UUID id, UUID ownerId, UUID accountId, LocalDate date, String merchant,
                                      String description, long amountCents, TransactionKind kind, String category,
                                      CategorySource categorySource, String source, String sourceFile,
                                       String sourceId, com.techie345.moneys.financial.ReviewStatus reviewStatus,
                                       com.techie345.moneys.financial.TransactionDirection direction,
                                       com.techie345.moneys.financial.TransferType transferType, Instant importedAt, String action,
                                       String symbol, java.math.BigDecimal shares, Long priceCents, long version) { }

    public record AssetRequest(@NotBlank String name, @NotNull com.techie345.moneys.financial.AssetKind kind,
                               @jakarta.validation.constraints.PositiveOrZero long valueCents) { }
    public record AssetResponse(UUID id, UUID ownerId, String name, com.techie345.moneys.financial.AssetKind kind, long valueCents, long version) { }
    public record HoldingRequest(@NotNull UUID accountId, @NotBlank String symbol, @jakarta.validation.constraints.PositiveOrZero @NotNull java.math.BigDecimal shares,
                                 @jakarta.validation.constraints.PositiveOrZero long costBasisCents, @jakarta.validation.constraints.PositiveOrZero long marketValueCents,
                                 String source, String sourceFile, Instant importedAt) { }
    public record HoldingResponse(UUID id, UUID ownerId, UUID accountId, String symbol, java.math.BigDecimal shares, long costBasisCents, long marketValueCents, String source, String sourceFile, Instant importedAt, long version) { }
    public record LiabilityRequest(@NotBlank String name, @NotNull com.techie345.moneys.financial.LiabilityKind kind, @jakarta.validation.constraints.Positive long balanceCents,
                                   java.math.BigDecimal interestRate, @jakarta.validation.constraints.PositiveOrZero Long minimumPaymentCents, UUID accountId, String source, String sourceFile, Instant importedAt) { }
    public record LiabilityResponse(UUID id, UUID ownerId, String name, com.techie345.moneys.financial.LiabilityKind kind, long balanceCents, java.math.BigDecimal interestRate, Long minimumPaymentCents, UUID accountId, String source, String sourceFile, Instant importedAt, long version) { }
    public record ObligationRequest(@NotBlank String name, @jakarta.validation.constraints.Pattern(regexp = "^(housing|food|transportation|utilities|healthcare|shopping|entertainment|subscriptions|salary|other|transfer)$") String category, @jakarta.validation.constraints.PositiveOrZero long amountCents,
                                     @jakarta.validation.constraints.Pattern(regexp = "^(weekly|monthly|yearly)$") String frequency, LocalDate nextDueDate) { }
    public record ObligationResponse(UUID id, UUID ownerId, String name, String category, long amountCents, String frequency, LocalDate nextDueDate, long version) { }
    public record GoalRequest(@NotBlank String name, @jakarta.validation.constraints.PositiveOrZero long targetCents, @jakarta.validation.constraints.PositiveOrZero long currentCents, LocalDate deadline) { }
    public record GoalResponse(UUID id, UUID ownerId, String name, long targetCents, long currentCents, LocalDate deadline, long version) { }
    public record RuleRequest(@NotBlank String pattern, @jakarta.validation.constraints.Pattern(regexp = "^(housing|food|transportation|utilities|healthcare|shopping|entertainment|subscriptions|salary|other|transfer)$") String category, UUID accountId, TransactionKind kind) { }
    public record RuleResponse(UUID id, UUID ownerId, String pattern, String category, UUID accountId, TransactionKind kind, long version) { }
    public record SnapshotResponse(UUID id, UUID ownerId, LocalDate date, long assetsCents, long liabilitiesCents, long netWorthCents, UUID accountId, String source, String sourceFile, Instant importedAt, long version) { }
    public record SnapshotRequest(@NotNull LocalDate date,
                                  @jakarta.validation.constraints.PositiveOrZero long assetsCents,
                                  @jakarta.validation.constraints.PositiveOrZero long liabilitiesCents,
                                  long netWorthCents, UUID accountId, String source, String sourceFile, Instant importedAt) { }
}
