package com.techie345.moneys.sync;

import com.techie345.moneys.audit.ChangeEventEntity;
import com.techie345.moneys.audit.ChangeEventRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.database.enabled", havingValue = "true", matchIfMissing = true)
public class SyncService {
    public static final int DEFAULT_LIMIT = 100;
    public static final int MAX_LIMIT = 500;
    private final ChangeEventRepository events;
    public SyncService(ChangeEventRepository events) { this.events = events; }

    @Transactional(readOnly = true)
    public SyncResponse read(UUID ownerId, long cursor) {
        return read(ownerId, cursor, DEFAULT_LIMIT);
    }

    @Transactional(readOnly = true)
    public SyncResponse read(UUID ownerId, long cursor, int limit) {
        if (cursor < 0) throw new IllegalArgumentException("cursor must not be negative");
        if (limit < 1 || limit > MAX_LIMIT) throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT);
        var changes = events.findByOwnerIdAndSequenceGreaterThanOrderBySequenceAsc(ownerId, cursor, PageRequest.of(0, limit))
                .stream().map(SyncService::change).toList();
        return new SyncResponse(changes, changes.isEmpty() ? cursor : changes.getLast().sequence());
    }

    private static SyncResponse.Change change(ChangeEventEntity event) {
        return new SyncResponse.Change(event.getSequence(), event.getOperation(), event.getResourceType(),
                event.getResourceId(), event.getSourceId(), event.getImportId(), event.getChangedAt(),
                event.getVersion(), event.getPayload());
    }
}
