package com.techie345.moneys.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.UUID;
import com.techie345.moneys.financial.AssetKind;
import com.techie345.moneys.financial.asset.AssetEntity;
import org.junit.jupiter.api.Test;

class AuditServiceTest {
    @Test
    void recordsAllChangeMetadataIncludingTombstone() {
        ChangeEventRepository repository = mock(ChangeEventRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        UUID owner = UUID.randomUUID();
        UUID resource = UUID.randomUUID();
        UUID importId = UUID.randomUUID();

        ChangeEventEntity event = new AuditService(repository).record(owner, ChangeOperation.DELETED,
                "transaction", resource, "source-1", importId, 7, "{\"id\":\"resource\"}");

        assertThat(event.getOwnerId()).isEqualTo(owner);
        assertThat(event.getOperation()).isEqualTo(ChangeOperation.DELETED);
        assertThat(event.getResourceType()).isEqualTo("transaction");
        assertThat(event.getResourceId()).isEqualTo(resource);
        assertThat(event.getSourceId()).isEqualTo("source-1");
        assertThat(event.getImportId()).isEqualTo(importId);
        assertThat(event.getVersion()).isEqualTo(7);
        assertThat(event.getPayload()).isEqualTo("{\"id\":\"resource\"}");
        verify(repository).save(event);
    }

    @Test
    void rejectsEmptyAuditPayloads() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                new AuditService(mock(ChangeEventRepository.class)).record(UUID.randomUUID(),
                        ChangeOperation.CREATED, "account", UUID.randomUUID(), null, null, 0, "{}"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createsSafeNonEmptySnapshotWithAuditMetadata() {
        UUID owner = UUID.randomUUID();
        UUID importId = UUID.randomUUID();
        AssetEntity asset = new AssetEntity(owner, "Home", AssetKind.PROPERTY, 100);

        String payload = AuditService.snapshot(asset, importId);

        assertThat(payload).contains("ownerId", "version", importId.toString());
        assertThat(payload).doesNotContain("description", "normalizedJson", "confirmationToken");
    }
}
