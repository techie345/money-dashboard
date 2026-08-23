package com.techie345.moneys.financial;
import java.util.Map;
public final class CalculationReadModels { private CalculationReadModels() {}
 public record Overview(long incomeCents,long spendingCents,long netWorthCents,double savingsRate) {}
 public record Spending(long totalCents,Map<String,Long> byCategory) {}
 public record Income(long totalCents,Map<String,Long> byCategory) {}
 public record Investing(long holdingsCents,long contributionsCents) {}
}
