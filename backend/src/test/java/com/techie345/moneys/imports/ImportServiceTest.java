package com.techie345.moneys.imports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.mockito.InOrder;

import com.techie345.moneys.financial.CategorySource;
import com.techie345.moneys.financial.TransactionKind;
import com.techie345.moneys.financial.account.AccountRepository;
import com.techie345.moneys.financial.rule.RuleRepository;
import com.techie345.moneys.financial.transaction.TransactionRepository;
import com.techie345.moneys.financial.transaction.TransactionEntity;
import com.techie345.moneys.imports.provider.*;
import com.techie345.moneys.audit.AuditService;
import com.techie345.moneys.audit.ChangeOperation;
import java.time.Instant;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class ImportServiceTest {
    private final ImportRepository imports = mock(ImportRepository.class);
    private final ImportAuditRepository audits = mock(ImportAuditRepository.class);
    private final TransactionRepository transactions = mock(TransactionRepository.class);
    private final RuleRepository rules = mock(RuleRepository.class);
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final ProviderRegistry registry = mock(ProviderRegistry.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final ImportService service = new ImportService(imports, audits, transactions, rules, accounts, registry, mapper);
    private final AuditService audit = mock(AuditService.class);
    private final UUID owner = UUID.randomUUID();
    private final UUID account = UUID.randomUUID();

    @Test
    void previewDoesNotTouchCanonicalRepositories() throws Exception {
        ImportEntity staged = staged("PREVIEW", normalized(candidate(null), List.of()));
        when(imports.findByIdAndOwnerId(staged.getId(), owner)).thenReturn(java.util.Optional.of(staged));

        var response = service.preview(staged.getId(), owner);

        assertEquals(1, response.preview().candidates().size());
        verifyNoInteractions(accounts, transactions, audits, rules);
    }

    @Test
    void discardWritesOneImportTombstoneWithImportReference() throws Exception {
        ImportEntity staged = staged("PREVIEW", normalized(candidate(null), List.of()));
        when(imports.findByIdAndOwnerId(staged.getId(), owner)).thenReturn(java.util.Optional.of(staged));
        ImportService audited = new ImportService(imports, audits, transactions, rules, accounts, registry, mapper, audit);

        audited.discard(staged.getId(), owner);

        InOrder order = inOrder(staged, imports, audit);
        order.verify(staged).discard();
        order.verify(imports).saveAndFlush(staged);
        order.verify(audit).recordEntity(staged, ChangeOperation.DELETED, staged.getId());
        verify(audit, times(1)).recordEntity(any(), any(), any());
    }

    @Test
    void commitRejectsAccountOwnedByAnotherUserBeforeClaimingStaging() throws Exception {
        ImportEntity staged = staged("PREVIEW", normalized(candidate(null), List.of()));
        when(imports.findByIdAndOwnerId(staged.getId(), owner)).thenReturn(java.util.Optional.of(staged));
        when(accounts.findByIdAndOwnerId(account, owner)).thenReturn(java.util.Optional.empty());

        assertEquals("IMPORT_ACCOUNT_REQUIRED", assertThrows(ImportConfirmationException.class,
                () -> service.commit(staged.getId(), owner, "token", 0)).getMessage());
        verify(imports, never()).claimForCommit(any(), any(), anyLong());
        verifyNoInteractions(transactions, audits);
    }

    @Test
    void commitRejectsStagedDuplicatesWithoutWritingCanonicalData() throws Exception {
        ImportEntity staged = staged("PREVIEW", normalized(candidate(account), List.of(new DuplicateCandidate(2, 1, "same source identifier"))));
        when(imports.findByIdAndOwnerId(staged.getId(), owner)).thenReturn(java.util.Optional.of(staged));

        assertEquals("IMPORT_DUPLICATES_FOUND", assertThrows(ImportConfirmationException.class,
                () -> service.commit(staged.getId(), owner, "token", 0)).getMessage());
        verifyNoInteractions(accounts, transactions, audits);
    }

    @Test
    void repeatedCommitReturnsTheSameFullPreviewShape() throws Exception {
        ImportEntity staged = staged("COMMITTED", normalized(candidate(account), List.of()));
        when(imports.findByIdAndOwnerId(staged.getId(), owner)).thenReturn(java.util.Optional.of(staged));

        var response = service.commit(staged.getId(), owner, "wrong", 99);

        assertEquals(1, response.preview().candidates().size());
        assertEquals("COMMITTED", response.status());
        verifyNoInteractions(accounts, transactions, audits);
    }

    @Test
    void commitRechecksExistingCanonicalDuplicates() throws Exception {
        ImportEntity staged = staged("PREVIEW", normalized(candidate(account), List.of()));
        when(imports.findByIdAndOwnerId(staged.getId(), owner)).thenReturn(java.util.Optional.of(staged));
        when(accounts.findByIdAndOwnerId(account, owner)).thenReturn(java.util.Optional.of(mock(com.techie345.moneys.financial.account.AccountEntity.class)));
        when(transactions.findAllByOwnerId(owner)).thenReturn(List.of(new TransactionEntity(owner, account,
                LocalDate.of(2026, 1, 1), "Store", "Store", 100, TransactionKind.SPENDING, "Food",
                CategorySource.IMPORTED, "csv", "file.csv", "source-1")));

        assertEquals("IMPORT_DUPLICATES_FOUND", assertThrows(ImportConfirmationException.class,
                () -> service.commit(staged.getId(), owner, "token", 0)).getMessage());
        verify(imports, never()).claimForCommit(any(), any(), anyLong());
    }

    @Test
    void previewRejectsExpiredStaging() throws Exception {
        ImportEntity staged = staged("PREVIEW", normalized(candidate(null), List.of()));
        when(staged.getExpiresAt()).thenReturn(Instant.now().minus(Duration.ofMinutes(1)));
        when(imports.findByIdAndOwnerId(staged.getId(), owner)).thenReturn(java.util.Optional.of(staged));

        assertEquals("IMPORT_EXPIRED", assertThrows(ImportConfirmationException.class,
                () -> service.preview(staged.getId(), owner)).getMessage());
    }

    @Test
    void commitRejectsExpiredStaging() throws Exception {
        ImportEntity staged = staged("PREVIEW", normalized(candidate(account), List.of()));
        when(staged.getExpiresAt()).thenReturn(Instant.now().minus(Duration.ofMinutes(1)));
        when(imports.findByIdAndOwnerId(staged.getId(), owner)).thenReturn(java.util.Optional.of(staged));

        assertEquals("IMPORT_EXPIRED", assertThrows(ImportConfirmationException.class,
                () -> service.commit(staged.getId(), owner, "token", 0)).getMessage());
        verify(imports, never()).claimForCommit(any(), any(), anyLong());
    }

    @Test
    void concurrentSourceIdConflictIsReturnedAsDuplicate() throws Exception {
        ImportEntity staged = staged("PREVIEW", normalized(candidate(account), List.of()));
        when(imports.findByIdAndOwnerId(staged.getId(), owner)).thenReturn(java.util.Optional.of(staged));
        when(accounts.findByIdAndOwnerId(account, owner)).thenReturn(java.util.Optional.of(mock(com.techie345.moneys.financial.account.AccountEntity.class)));
        when(transactions.findAllByOwnerId(owner)).thenReturn(List.of());
        when(imports.claimForCommit(staged.getId(), owner, 0)).thenReturn(1);
        doThrow(new org.springframework.dao.DataIntegrityViolationException("uq_transaction_owner_source_id"))
                .when(transactions).saveAndFlush(any());

        assertEquals("IMPORT_DUPLICATES_FOUND", assertThrows(ImportConfirmationException.class,
                () -> service.commit(staged.getId(), owner, "token", 0)).getMessage());
    }

    private NormalizedCandidate candidate(UUID accountId) {
        return new NormalizedCandidate(1, accountId, LocalDate.of(2026, 1, 1), "Store", "Store", 100,
                TransactionKind.SPENDING, "Food", CategorySource.IMPORTED, "source-1");
    }

    private NormalizedImport normalized(NormalizedCandidate candidate, List<DuplicateCandidate> duplicates) {
        return new NormalizedImport(new ProviderMetadata("file.csv", "generic", Instant.parse("2026-08-23T10:00:00Z"), "csv"),
                List.of(candidate), List.of(), duplicates);
    }

    private ImportEntity staged(String status, NormalizedImport normalized) throws Exception {
        ImportEntity staged = mock(ImportEntity.class);
        var id = UUID.randomUUID();
        when(staged.getId()).thenReturn(id);
        when(staged.getStatus()).thenReturn(status);
        when(staged.getVersion()).thenReturn(0L);
        when(staged.getOwnerId()).thenReturn(owner);
        when(staged.getFilename()).thenReturn("file.csv");
        when(staged.getProfile()).thenReturn("generic");
        when(staged.getImportedAt()).thenReturn(normalized.metadata().importedAt());
        when(staged.getConfirmationToken()).thenReturn("token");
        when(staged.getNormalizedJson()).thenReturn(mapper.writeValueAsString(normalized));
        when(staged.getExpiresAt()).thenReturn(Instant.now().plus(Duration.ofMinutes(30)));
        return staged;
    }
}
