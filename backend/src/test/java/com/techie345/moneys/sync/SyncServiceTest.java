package com.techie345.moneys.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.techie345.moneys.audit.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class SyncServiceTest {
    @Test
    void returnsOnlyChangesAfterOwnerCursorAndAdvancesToLastSequence() {
        ChangeEventRepository repository = mock(ChangeEventRepository.class);
        UUID owner = UUID.randomUUID();
        ChangeEventEntity event = new ChangeEventEntity(owner, ChangeOperation.UPDATED, "account",
                UUID.randomUUID(), null, null, 2, "{\"name\":\"Checking\"}");
        when(repository.findByOwnerIdAndSequenceGreaterThanOrderBySequenceAsc(owner, 4L, PageRequest.of(0, 100))).thenReturn(List.of(event));

        SyncResponse response = new SyncService(repository).read(owner, 4L);

        assertThat(response.changes()).hasSize(1);
        assertThat(response.changes().getFirst().operation()).isEqualTo(ChangeOperation.UPDATED);
        assertThat(response.nextCursor()).isEqualTo(event.getSequence());
        verify(repository).findByOwnerIdAndSequenceGreaterThanOrderBySequenceAsc(owner, 4L, PageRequest.of(0, 100));
    }

    @Test
    void appliesConfiguredPageLimit() {
        ChangeEventRepository repository = mock(ChangeEventRepository.class);
        UUID owner = UUID.randomUUID();
        when(repository.findByOwnerIdAndSequenceGreaterThanOrderBySequenceAsc(owner, 0L, PageRequest.of(0, 2)))
                .thenReturn(List.of());

        new SyncService(repository).read(owner, 0L, 2);

        verify(repository).findByOwnerIdAndSequenceGreaterThanOrderBySequenceAsc(owner, 0L, PageRequest.of(0, 2));
    }

    @Test
    void rejectsOutOfRangePageLimit() {
        ChangeEventRepository repository = mock(ChangeEventRepository.class);

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                new SyncService(repository).read(UUID.randomUUID(), 0L, SyncService.MAX_LIMIT + 1))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }
}
