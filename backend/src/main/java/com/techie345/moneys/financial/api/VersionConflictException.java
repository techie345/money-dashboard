package com.techie345.moneys.financial.api;

public class VersionConflictException extends RuntimeException {
    private final Object currentRepresentation;

    public VersionConflictException() {
        this(null);
    }

    public VersionConflictException(Object currentRepresentation) {
        super("resource version is stale");
        this.currentRepresentation = currentRepresentation;
    }

    public Object getCurrentRepresentation() {
        return currentRepresentation;
    }
}
