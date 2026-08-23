package com.techie345.moneys.financial;

import java.util.UUID;

import com.techie345.moneys.financial.account.AccountRepository;
import com.techie345.moneys.financial.api.FinancialResourceController;
import com.techie345.moneys.financial.transaction.TransactionRepository;
import com.techie345.moneys.financial.asset.AssetRepository;
import com.techie345.moneys.financial.holding.HoldingRepository;
import com.techie345.moneys.financial.liability.LiabilityRepository;
import com.techie345.moneys.financial.obligation.ObligationRepository;
import com.techie345.moneys.financial.goal.GoalRepository;
import com.techie345.moneys.financial.rule.RuleRepository;
import com.techie345.moneys.financial.snapshot.SnapshotRepository;
import com.techie345.moneys.financial.snapshot.SnapshotEntity;
import com.techie345.moneys.financial.FinancialMutationService;
import com.techie345.moneys.financial.FinancialCalculationService;
import com.techie345.moneys.identity.CurrentUserArgumentResolver;
import org.mockito.Mockito;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import java.util.List;
import java.util.Optional;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.mockito.Mockito.when;
import com.techie345.moneys.financial.transaction.TransactionEntity;

@WebMvcTest(FinancialResourceController.class)
@AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.context.TestPropertySource(properties = {
        "app.financial.api.enabled=true",
        "app.database.enabled=false"
})
@ActiveProfiles("test")
@org.springframework.context.annotation.Import({FinancialResourceControllerTest.RepositoryConfiguration.class,
        CurrentUserArgumentResolver.class})
class FinancialResourceControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private SnapshotRepository snapshotRepository;

    @Test
    void rejectsInvalidAccountAmountWithoutPersistence() throws Exception {
        UUID ownerId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/accounts")
                        .principal(() -> ownerId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"institution\":\"Bank\",\"name\":\"Checking\",\"kind\":\"cash\",\"balanceCents\":\"not-a-number\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchesTransactionWithMatchingVersion() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        TransactionEntity transaction = new TransactionEntity(ownerId,
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                java.time.LocalDate.of(2025, 1, 1), "Old", "Old", 100,
                TransactionKind.SPENDING, "old", CategorySource.IMPORTED,
                "manual", "manual", null);
        when(transactionRepository.findByIdAndOwnerId(transactionId, ownerId)).thenReturn(Optional.of(transaction));
        when(accountRepository.findByIdAndOwnerId(Mockito.any(), Mockito.eq(ownerId)))
                .thenReturn(Optional.of(new com.techie345.moneys.financial.account.AccountEntity(ownerId, "Bank", "Checking", AccountKind.CASH, 0)));
        when(transactionRepository.save(Mockito.any())).thenAnswer(invocation -> invocation.getArgument(0));
        mockMvc.perform(patch("/api/v1/transactions/{id}", transactionId)
                        .principal(() -> ownerId.toString())
                        .header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-01-01\",\"merchant\":\"Store\",\"description\":\"Groceries\",\"amountCents\":1200,\"kind\":\"spending\",\"category\":\"food\",\"accountId\":\"00000000-0000-0000-0000-000000000001\",\"source\":\"manual\",\"sourceFile\":\"manual\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"0\""));
    }

    @Test
    void createsAssetForCurrentUser() throws Exception {
        UUID ownerId = UUID.randomUUID();
        when(assetRepository.save(Mockito.any())).thenAnswer(invocation -> invocation.getArgument(0));
        mockMvc.perform(post("/api/v1/assets")
                        .principal(() -> ownerId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Home\",\"kind\":\"property\",\"valueCents\":25000000}"))
                .andExpect(status().isCreated());
    }

    @Test
    void createsSnapshotWithOwnerScopedMetadata() throws Exception {
        UUID ownerId = UUID.randomUUID();
        when(snapshotRepository.save(Mockito.any())).thenAnswer(invocation -> invocation.getArgument(0));
        mockMvc.perform(post("/api/v1/net-worth-snapshots").principal(() -> ownerId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-01-01\",\"assetsCents\":1000,\"liabilitiesCents\":250,\"netWorthCents\":750}"))
                .andExpect(status().isCreated());
    }

    @Test
    void updatesSnapshotWithStrongIfMatchAndEmitsEtag() throws Exception {
        UUID ownerId = UUID.randomUUID(); UUID snapshotId = UUID.randomUUID();
        SnapshotEntity existing = new SnapshotEntity(ownerId, java.time.LocalDate.of(2026, 1, 1), 1000, 250, 750, null, null, null, null);
        when(snapshotRepository.findByIdAndOwnerId(snapshotId, ownerId)).thenReturn(Optional.of(existing));
        when(snapshotRepository.save(Mockito.any())).thenAnswer(invocation -> invocation.getArgument(0));
        mockMvc.perform(patch("/api/v1/net-worth-snapshots/{id}", snapshotId).principal(() -> ownerId.toString())
                        .header("If-Match", "\"0\"").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-02-01\",\"assetsCents\":1200,\"liabilitiesCents\":300,\"netWorthCents\":900}"))
                .andExpect(status().isOk()).andExpect(header().string("ETag", "\"0\""));
    }

    @Test
    void hidesSnapshotOwnedByAnotherUserAndRequiresIfMatchForDelete() throws Exception {
        UUID ownerId = UUID.randomUUID(); UUID snapshotId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/net-worth-snapshots/{id}", snapshotId).principal(() -> ownerId.toString()))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/net-worth-snapshots/{id}", snapshotId).principal(() -> ownerId.toString()))
                .andExpect(status().isBadRequest());
    }

    @TestConfiguration
    static class RepositoryConfiguration implements WebMvcConfigurer {
        @Bean
        AccountRepository accountRepository() {
            return Mockito.mock(AccountRepository.class);
        }

        @Bean
        TransactionRepository transactionRepository() {
            return Mockito.mock(TransactionRepository.class);
        }

        @Bean AssetRepository assetRepository() { return Mockito.mock(AssetRepository.class); }
        @Bean HoldingRepository holdingRepository() { return Mockito.mock(HoldingRepository.class); }
        @Bean LiabilityRepository liabilityRepository() { return Mockito.mock(LiabilityRepository.class); }
        @Bean ObligationRepository obligationRepository() { return Mockito.mock(ObligationRepository.class); }
        @Bean GoalRepository goalRepository() { return Mockito.mock(GoalRepository.class); }
        @Bean RuleRepository ruleRepository() { return Mockito.mock(RuleRepository.class); }
        @Bean SnapshotRepository snapshotRepository() { return Mockito.mock(SnapshotRepository.class); }
        @Bean FinancialMutationService financialMutationService() { return new FinancialMutationService(); }
        @Bean FinancialCalculationService financialCalculationService() { return Mockito.mock(FinancialCalculationService.class); }

        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new CurrentUserArgumentResolver());
        }
    }
}
