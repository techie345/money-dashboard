package com.techie345.moneys.audit;

import java.util.UUID;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.database.enabled", havingValue = "true", matchIfMissing = true)
public class AuditService {
    private final ChangeEventRepository events;

    public AuditService(ChangeEventRepository events) { this.events = events; }

    @Transactional
    public ChangeEventEntity record(UUID ownerId, ChangeOperation operation, String resourceType, UUID resourceId,
                                     String sourceId, UUID importId, long version, String payload) {
        if (ownerId == null || payload == null || payload.trim().equals("{}") || !isJsonObject(payload))
            throw new IllegalArgumentException("audit event requires an owner, resource, and payload");
        long sequence = events.nextSequence(ownerId);
        return events.save(new ChangeEventEntity(ownerId, sequence, operation, resourceType, resourceId,
                sourceId, importId, version, payload));
    }

    public ChangeEventEntity recordEntity(Object entity, ChangeOperation operation, UUID importId) {
        return recordEntity(entity, operation, importId, Map.of());
    }

    public ChangeEventEntity recordEntity(Object entity, ChangeOperation operation, UUID importId,
                                          Map<String, Object> additionalSnapshotFields) {
        UUID ownerId = (UUID) value(entity, "getOwnerId");
        UUID resourceId = (UUID) value(entity, "getId");
        long version = ((Number) value(entity, "getVersion")).longValue();
        String sourceId = (String) value(entity, "getSourceId");
        String type = entity.getClass().getSimpleName().replace("Entity", "").toLowerCase(java.util.Locale.ROOT);
        return record(ownerId, operation, type, resourceId, sourceId, importId, version,
                snapshot(entity, importId, additionalSnapshotFields));
    }

    public static String snapshot(Object entity, UUID importId) {
        return snapshot(entity, importId, Map.of());
    }

    private static String snapshot(Object entity, UUID importId, Map<String, Object> additionalSnapshotFields) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        for (String getter : List.of("getId", "getOwnerId", "getVersion", "getInstitution", "getName", "getKind",
                "getBalanceCents", "getAccountId", "getDate", "getMerchant", "getAmountCents", "getCategory",
                "getCategorySource", "getSource", "getSourceFile", "getSourceId", "getReviewStatus", "getDirection",
                "getTransferType", "getImportedAt", "getAction", "getSymbol", "getShares", "getPriceCents",
                "getFilename", "getProfile", "getStatus", "getExpiresAt")) {
            Object value = value(entity, getter);
            if (value != null) snapshot.put(getter.substring(3, 4).toLowerCase(java.util.Locale.ROOT) + getter.substring(4), value);
        }
        if (importId != null) snapshot.put("importId", importId);
        snapshot.putAll(additionalSnapshotFields);
        try {
            return new ObjectMapper().writeValueAsString(snapshot);
        } catch (Exception exception) {
            throw new IllegalStateException("could not serialize audit snapshot", exception);
        }
    }

    private static Object value(Object entity, String getter) {
        try { return entity.getClass().getMethod(getter).invoke(entity); }
        catch (ReflectiveOperationException ignored) { return null; }
    }

    private static boolean isJsonObject(String payload) {
        try {
            Object parsed = new ObjectMapper().readValue(payload, Map.class);
            return parsed instanceof Map<?, ?> map && !map.isEmpty();
        } catch (Exception ignored) {
            return false;
        }
    }
}
