package com.techie345.moneys.financial;

import java.util.List;

@org.springframework.modulith.NamedInterface("import")
public final class MerchantRules {
    private MerchantRules() {
    }

    public static List<TransactionRecord> apply(List<TransactionRecord> transactions, List<MerchantRuleRecord> rules) {
        return transactions.stream().map(transaction -> {
            if (transaction.categorySource() == CategorySource.MANUAL) return transaction;
            return rules.stream()
                    .filter(rule -> rule.accountId() == null || rule.accountId().equals(transaction.accountId()))
                    .filter(rule -> rule.kind() == null || rule.kind() == transaction.kind())
                    .filter(rule -> matches(transaction.merchant(), rule))
                    .findFirst()
                    .map(rule -> transaction.withCategory(rule.category(), CategorySource.RULE))
                    .orElse(transaction);
        }).toList();
    }

    public static String normalize(String value) {
        String normalized = value.toLowerCase().replaceAll("[\\u0000-\\u002f\\u003a-\\u0040\\u005b-\\u0060\\u007b-\\u007f]", " ")
                .replaceAll("\\s+", " ").trim().replaceFirst("^(sq|tst|paypal)\\s+", "").replaceFirst("\\s+#?\\d+$", "");
        while (normalized.matches(".*\\s+(pos|purchase|debit|credit|payment|pmt|inc|llc|ltd)$")) normalized = normalized.replaceFirst("\\s+(pos|purchase|debit|credit|payment|pmt|inc|llc|ltd)$", "");
        return normalized.trim();
    }
    public static boolean matches(String merchant, MerchantRuleRecord rule) { return java.util.Arrays.stream(rule.pattern().split("\\|"))
            .map(MerchantRules::normalize).filter(s -> !s.isEmpty()).anyMatch(normalize(merchant)::contains); }
}
