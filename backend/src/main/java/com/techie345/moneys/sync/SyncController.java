package com.techie345.moneys.sync;

import com.techie345.moneys.identity.CurrentUser;
import com.techie345.moneys.financial.api.FinancialResourceDtos.ApiError;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sync")
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.database.enabled", havingValue = "true", matchIfMissing = true)
public class SyncController {
    private final SyncService sync;
    public SyncController(SyncService sync) { this.sync = sync; }

    @GetMapping
    public SyncResponse sync(@RequestParam(defaultValue = "0") String cursor,
                             @RequestParam(defaultValue = "100") String limit, CurrentUser currentUser) {
        try {
            return sync.read(currentUser.id(), Long.parseLong(cursor), Integer.parseInt(limit));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("cursor and limit must be integers", exception);
        }
    }

    SyncResponse sync(long cursor, int limit, CurrentUser currentUser) {
        return sync.read(currentUser.id(), cursor, limit);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> invalidCursor() {
        return ResponseEntity.badRequest().body(new ApiError(400, "INVALID_CURSOR", "cursor and limit are invalid",
                List.of(), UUID.randomUUID().toString(), Instant.now()));
    }
}
