package com.techie345.moneys.financial;

import java.util.UUID;

@org.springframework.modulith.NamedInterface("import")
public record MerchantRuleRecord(String pattern, String category, UUID accountId, TransactionKind kind) {
}
