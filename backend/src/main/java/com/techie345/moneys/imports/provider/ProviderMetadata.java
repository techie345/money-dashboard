package com.techie345.moneys.imports.provider;
import java.time.Instant;
public record ProviderMetadata(String filename, String profile, Instant importedAt, String source) { }
