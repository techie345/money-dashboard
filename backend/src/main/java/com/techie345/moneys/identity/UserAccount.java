package com.techie345.moneys.identity;

import java.time.Instant;
import java.util.UUID;

public record UserAccount(
        UUID id,
        String googleSubject,
        String displayName,
        String email,
        Instant createdAt,
        Instant updatedAt
) {
}
