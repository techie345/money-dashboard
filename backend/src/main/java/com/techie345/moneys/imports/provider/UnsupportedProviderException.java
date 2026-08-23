package com.techie345.moneys.imports.provider;

public class UnsupportedProviderException extends RuntimeException {
    public UnsupportedProviderException(ProviderType type) {
        super("unsupported provider: " + type);
    }
}
