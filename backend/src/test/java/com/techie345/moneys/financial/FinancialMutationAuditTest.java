package com.techie345.moneys.financial;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.techie345.moneys.audit.*;
import com.techie345.moneys.financial.asset.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class FinancialMutationAuditTest {
    @Test
    void recordsCreateUpdateAndDeleteForOwnedFinancialEntities() {
        AssetRepository repository = mock(AssetRepository.class);
        AuditService audit = mock(AuditService.class);
        AssetEntity asset = new AssetEntity(UUID.randomUUID(), "Home", AssetKind.PROPERTY, 100);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FinancialMutationService mutations = new FinancialMutationService(audit);
        mutations.create(repository, asset);
        mutations.update(() -> asset, 0, value -> value.update("House", AssetKind.PROPERTY, 200), repository);
        mutations.delete(() -> asset, 0, AssetEntity::getVersion, repository);

        verify(audit).recordEntity(asset, ChangeOperation.CREATED, null);
        verify(audit).recordEntity(asset, ChangeOperation.UPDATED, null);
        verify(audit).recordEntity(asset, ChangeOperation.DELETED, null);
        verify(audit, times(3)).recordEntity(any(), any(), isNull());
    }

    @Test
    void writesDeleteTombstoneBeforeDeletingResource() {
        AssetRepository repository = mock(AssetRepository.class);
        AuditService audit = mock(AuditService.class);
        AssetEntity asset = new AssetEntity(UUID.randomUUID(), "Home", AssetKind.PROPERTY, 100);

        new FinancialMutationService(audit).delete(() -> asset, 0, AssetEntity::getVersion, repository);

        InOrder order = inOrder(audit, repository);
        order.verify(audit).recordEntity(asset, ChangeOperation.DELETED, null);
        order.verify(repository).delete(asset);
    }
}
