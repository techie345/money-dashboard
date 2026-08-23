package com.techie345.moneys.sync;

import com.techie345.moneys.audit.ChangeOperation;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SyncResponse(List<Change> changes, long nextCursor) {
    public record Change(long sequence, ChangeOperation operation, String resourceType, UUID resourceId,
                         String sourceId, UUID importId, Instant changedAt, long version, String payload) { }
}
