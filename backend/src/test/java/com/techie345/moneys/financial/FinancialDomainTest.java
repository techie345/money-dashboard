package com.techie345.moneys.financial;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FinancialDomainTest {
    private static final UUID ACCOUNT_ID = UUID.randomUUID();

    @Test
    void calculatesSpendingAndIncomeWithoutTransfersOrCardPayments() {
        List<TransactionRecord> transactions = List.of(
                transaction(1000, TransactionKind.SPENDING, "food"),
                transaction(250, TransactionKind.REFUND, "food"),
                transaction(900, TransactionKind.INCOME, "salary"),
                transaction(5000, TransactionKind.TRANSFER, "transfer"),
                transaction(3000, TransactionKind.CREDIT_CARD_PAYMENT, "transfer"));

        assertThat(FinancialCalculations.sumSpending(transactions)).isEqualTo(750);
        assertThat(FinancialCalculations.sumIncome(transactions)).isEqualTo(900);
    }

    @Test
    void rejectsNonPositiveMoneyMagnitude() {
        assertThatThrownBy(() -> transaction(0, TransactionKind.SPENDING, "food"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void preservesManualCategoriesWhenRulesApply() {
        TransactionRecord manual = transaction(100, TransactionKind.SPENDING, "manual")
                .withMerchant("Coffee Shop").withCategorySource(CategorySource.MANUAL);
        TransactionRecord imported = transaction(100, TransactionKind.SPENDING, "other")
                .withMerchant("Coffee Shop").withCategorySource(CategorySource.IMPORTED);
        MerchantRuleRecord rule = new MerchantRuleRecord("coffee", "food", null, null);

        assertThat(MerchantRules.apply(List.of(manual, imported), List.of(rule)))
                .extracting(TransactionRecord::category)
                .containsExactly("manual", "food");
    }

    @Test
    void normalizesPaymentPrefixesSuffixesAndAlternativesLikeFrontend() {
        assertThat(MerchantRules.normalize("SQ Coffee #42 POS")).isEqualTo("coffee 42");
        assertThat(MerchantRules.normalize("coffee|coffee shop")).isEqualTo("coffee coffee shop");
        assertThat(MerchantRules.matches("PayPal Coffee Purchase", new MerchantRuleRecord("coffee", "food", null, null))).isTrue();
    }

    @Test
    void requiresReviewStatusOnExtendedTransactionRecord() {
        assertThatThrownBy(() -> new TransactionRecord(null, LocalDate.now(), "m", "d", 1,
                TransactionKind.SPENDING, "food", ACCOUNT_ID, CategorySource.IMPORTED, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static TransactionRecord transaction(long amount, TransactionKind kind, String category) {
        return TransactionRecord.create(LocalDate.of(2026, 1, 1), "Merchant", "Description",
                amount, kind, category, ACCOUNT_ID);
    }
}
