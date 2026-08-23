package com.techie345.moneys.financial;

import java.util.List;
import java.util.UUID;

public record FinancialDataset(
        List<AccountBalance> accounts,
        List<TransactionRecord> transactions,
        List<AssetValue> assets,
        List<HoldingValue> holdings,
        List<LiabilityValue> liabilities
) {
    public record AccountBalance(UUID id, String institution, String name, String kind, long balanceCents) {
        public AccountBalance(String kind, long balanceCents) { this(null, "", "", kind, balanceCents); }
    }
    public record AssetValue(long valueCents) { }
    public record HoldingValue(UUID accountId, long marketValueCents) { public HoldingValue(long value) { this(null, value); } }
    public record LiabilityValue(String kind, String name, UUID accountId, long balanceCents) { public LiabilityValue(long value) { this(null, "", null, value); } }
}
