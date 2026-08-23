package com.techie345.moneys.financial.api;

public class VersionConflictException extends RuntimeException {
    public VersionConflictException() {
        super("resource version is stale");
    }
}
