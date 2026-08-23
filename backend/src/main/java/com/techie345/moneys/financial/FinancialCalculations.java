package com.techie345.moneys.financial;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class FinancialCalculations {
    private FinancialCalculations() {
    }

    public static long sumSpending(List<TransactionRecord> transactions) {
        return transactions.stream().mapToLong(transaction -> switch (transaction.kind()) {
            case SPENDING -> transaction.amountCents();
            case REFUND -> -transaction.amountCents();
            default -> 0;
        }).sum();
    }

    public static long sumIncome(List<TransactionRecord> transactions) {
        return transactions.stream()
                .filter(transaction -> transaction.kind() == TransactionKind.INCOME)
                .mapToLong(TransactionRecord::amountCents)
                .sum();
    }

    public static double savingsRate(List<TransactionRecord> transactions) {
        long income = sumIncome(transactions);
        return income == 0 ? 0 : (double) (income - sumSpending(transactions)) / income;
    }

    public static Map<String, Long> monthlySpending(List<TransactionRecord> transactions) {
        return transactions.stream()
                .filter(transaction -> transaction.kind() == TransactionKind.SPENDING
                        || transaction.kind() == TransactionKind.REFUND)
                .collect(Collectors.groupingBy(transaction -> transaction.date().toString().substring(0, 7),
                        Collectors.summingLong(transaction -> transaction.kind() == TransactionKind.REFUND
                                ? -transaction.amountCents() : transaction.amountCents())));
    }

    public static long netWorthCents(FinancialDataset dataset) {
        var holdingAccountIds = dataset.holdings().stream().map(FinancialDataset.HoldingValue::accountId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        long accounts = dataset.accounts().stream()
                .filter(account -> account.kind().equals("cash") || account.kind().equals("investment") && !holdingAccountIds.contains(account.id()))
                .mapToLong(FinancialDataset.AccountBalance::balanceCents).sum();
        long holdings = dataset.holdings().stream().mapToLong(FinancialDataset.HoldingValue::marketValueCents).sum();
        long assets = dataset.assets().stream().mapToLong(FinancialDataset.AssetValue::valueCents).sum();
        long liabilities = dataset.accounts().stream()
                .filter(account -> account.kind().equals("credit") || account.kind().equals("loan"))
                .mapToLong(account -> Math.abs(account.balanceCents())).sum()
                + dataset.liabilities().stream().filter(liability -> dataset.accounts().stream().noneMatch(account -> liabilityMatchesAccount(liability, account)))
                .mapToLong(FinancialDataset.LiabilityValue::balanceCents).sum();
        return accounts + holdings + assets - liabilities;
    }

    private static boolean liabilityMatchesAccount(FinancialDataset.LiabilityValue liability, FinancialDataset.AccountBalance account) {
        if (liability.accountId() != null) return liability.accountId().equals(account.id());
        boolean credit = account.kind().equals("credit");
        boolean loan = account.kind().equals("loan");
        if (!credit && !loan) return false;
        if (credit != "credit_card".equals(liability.kind())) return false;
        var ignored = java.util.Set.of("account", "card", "cash", "back", "bank", "home", "loan", "the", "and", "for", "it");
        var liabilityWords = java.util.Arrays.stream(liability.name().toLowerCase().split("\\W+")).filter(s -> s.length() > 2 && !ignored.contains(s)).collect(Collectors.toSet());
        var accountWords = java.util.Arrays.stream((account.institution() + " " + account.name()).toLowerCase().split("\\W+")).filter(s -> s.length() > 2 && !ignored.contains(s)).collect(Collectors.toSet());
        return liabilityWords.stream().anyMatch(accountWords::contains);
    }
}
