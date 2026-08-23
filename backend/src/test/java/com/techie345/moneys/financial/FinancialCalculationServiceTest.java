package com.techie345.moneys.financial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import com.techie345.moneys.financial.account.*;
import com.techie345.moneys.financial.transaction.*;
import com.techie345.moneys.financial.asset.*;
import com.techie345.moneys.financial.holding.*;
import com.techie345.moneys.financial.liability.*;

class FinancialCalculationServiceTest {
    @Test
    void overviewScopesToOwnerAndExcludesTransfers() {
        UUID owner = UUID.randomUUID();
        AccountRepository accounts = mock(AccountRepository.class); TransactionRepository transactions = mock(TransactionRepository.class);
        AssetRepository assets = mock(AssetRepository.class); HoldingRepository holdings = mock(HoldingRepository.class); LiabilityRepository liabilities = mock(LiabilityRepository.class);
        when(accounts.findAllByOwnerId(owner)).thenReturn(List.of());
        UUID account = UUID.randomUUID();
        when(transactions.findAllByOwnerId(owner)).thenReturn(List.of(
                new TransactionEntity(owner, account, java.time.LocalDate.now(), "Pay", "Pay", 1000, TransactionKind.INCOME, "salary", CategorySource.MANUAL, "m", "m", null),
                new TransactionEntity(owner, account, java.time.LocalDate.now(), "Shop", "Shop", 300, TransactionKind.SPENDING, "food", CategorySource.MANUAL, "m", "m", null),
                new TransactionEntity(owner, account, java.time.LocalDate.now(), "Move", "Move", 900, TransactionKind.TRANSFER, "transfer", CategorySource.MANUAL, "m", "m", null)));
        when(assets.findAllByOwnerId(owner)).thenReturn(List.of());
        when(holdings.findAllByOwnerId(owner)).thenReturn(List.of(new HoldingEntity(owner, account, "ABC", new java.math.BigDecimal("1"), 100, 100, null, null, null)));
        when(liabilities.findAllByOwnerId(owner)).thenReturn(List.of());
        var overview = new FinancialCalculationService(accounts, transactions, assets, holdings, liabilities).overview(owner);
        assertThat(overview.incomeCents()).isEqualTo(1000); assertThat(overview.spendingCents()).isEqualTo(300); assertThat(overview.netWorthCents()).isEqualTo(100);
    }

    @Test
    void overviewUsesRefundsAndInvestmentAccountFallback() {
        UUID owner = UUID.randomUUID(); UUID account = UUID.randomUUID();
        AccountRepository accounts = mock(AccountRepository.class); TransactionRepository transactions = mock(TransactionRepository.class);
        AssetRepository assets = mock(AssetRepository.class); HoldingRepository holdings = mock(HoldingRepository.class); LiabilityRepository liabilities = mock(LiabilityRepository.class);
        AccountEntity investment = new AccountEntity(owner, "Broker", "Fallback", AccountKind.INVESTMENT, 500);
        when(accounts.findAllByOwnerId(owner)).thenReturn(List.of(investment));
        when(transactions.findAllByOwnerId(owner)).thenReturn(List.of(
                new TransactionEntity(owner, account, java.time.LocalDate.now(), "Pay", "Pay", 1000, TransactionKind.INCOME, "salary", CategorySource.MANUAL, "m", "m", null),
                new TransactionEntity(owner, account, java.time.LocalDate.now(), "Shop", "Shop", 300, TransactionKind.SPENDING, "food", CategorySource.MANUAL, "m", "m", null),
                new TransactionEntity(owner, account, java.time.LocalDate.now(), "Refund", "Refund", 100, TransactionKind.REFUND, "food", CategorySource.MANUAL, "m", "m", null)));
        when(assets.findAllByOwnerId(owner)).thenReturn(List.of()); when(holdings.findAllByOwnerId(owner)).thenReturn(List.of()); when(liabilities.findAllByOwnerId(owner)).thenReturn(List.of());
        var overview = new FinancialCalculationService(accounts, transactions, assets, holdings, liabilities).overview(owner);
        assertThat(overview.spendingCents()).isEqualTo(200); assertThat(overview.netWorthCents()).isEqualTo(500);
    }
}
