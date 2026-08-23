package com.techie345.moneys.identity;

import java.util.UUID;

public record CurrentUser(UUID id) {
    public static CurrentUser fromName(String name) {
        try {
            return new CurrentUser(UUID.fromString(name));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("authenticated principal is not a local user", exception);
        }
    }
}
