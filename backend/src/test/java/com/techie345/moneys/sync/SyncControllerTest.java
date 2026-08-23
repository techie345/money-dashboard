package com.techie345.moneys.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.techie345.moneys.identity.CurrentUser;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SyncControllerTest {
    @Test
    void delegatesSyncToTheAuthenticatedOwner() {
        SyncService service = mock(SyncService.class);
        UUID owner = UUID.randomUUID();
        SyncResponse expected = new SyncResponse(List.of(), 8);
        when(service.read(owner, 8, 100)).thenReturn(expected);

        SyncResponse actual = new SyncController(service).sync(8, 100, new CurrentUser(owner));

        assertThat(actual).isSameAs(expected);
        verify(service).read(owner, 8, 100);
    }

    @Test
    void returnsStructuredErrorForInvalidCursor() {
        var response = new SyncController(mock(SyncService.class)).invalidCursor();

        assertThat(response.getBody().code()).isEqualTo("INVALID_CURSOR");
        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }
}
