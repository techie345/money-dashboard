package com.techie345.moneys.imports.provider;
import java.time.Instant;
import java.util.Locale;
import com.techie345.moneys.financial.*;
public abstract class AbstractFileProvider implements FinancialDataProvider {
    protected ProviderMetadata metadataFor(ProviderInput input) { return new ProviderMetadata(input.filename(), input.profile(), input.importedAt(), type().name().toLowerCase(Locale.ROOT)); }
    protected String text(byte[] content) { return new String(content, java.nio.charset.StandardCharsets.UTF_8).replace("\uFEFF", ""); }
    protected ProviderPreview previewOf(NormalizedImport value) { return new ProviderPreview(value.metadata(), value.candidates(), value.issues(), value.duplicates()); }
    protected Instant importedAt(ProviderInput input, ProviderContext context) { return input.importedAt() == null ? context.now() : input.importedAt(); }
    protected static String issueValue(String value) {
        if (value == null) return "";
        return value.length() <= 256 ? value : value.substring(0, 256);
    }
    protected static TransactionKind classifyKind(String explicit, String text, boolean negative, boolean hasInvestmentFields) {
        if (explicit != null && !explicit.isBlank()) return TransactionKind.valueOf(explicit.trim().toUpperCase(Locale.ROOT));
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
        if (hasInvestmentFields || normalized.contains("invest") || normalized.contains("buy shares") || normalized.contains("sell shares")) return TransactionKind.INVESTMENT;
        if (normalized.contains("transfer")) return TransactionKind.TRANSFER;
        if (normalized.contains("payment") || normalized.contains("pmt")) return TransactionKind.CREDIT_CARD_PAYMENT;
        return negative ? TransactionKind.SPENDING : TransactionKind.INCOME;
    }
    protected static TransferType transferType(String explicit, TransactionKind kind) {
        if (explicit != null && !explicit.isBlank()) return TransferType.valueOf(explicit.trim().toUpperCase(Locale.ROOT));
        return kind == TransactionKind.TRANSFER ? TransferType.ACCOUNT_TRANSFER
                : kind == TransactionKind.CREDIT_CARD_PAYMENT ? TransferType.CREDIT_CARD_PAYMENT : null;
    }
    protected static TransactionDirection direction(boolean negative) { return negative ? TransactionDirection.OUTFLOW : TransactionDirection.INFLOW; }
}
