package com.techie345.moneys.imports.provider;
import com.techie345.moneys.financial.MerchantRuleRecord;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
public record ProviderContext(UUID ownerId, UUID accountId, List<MerchantRuleRecord> rules, Instant now) { }
