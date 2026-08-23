package com.techie345.moneys.imports.provider;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
public record ProviderInput(String filename, String profile, Instant importedAt, byte[] content, List<ManualEntry> entries, ProviderType type, UUID accountId) {
    public static ProviderInput file(String filename, String profile, Instant importedAt, byte[] content) { return new ProviderInput(filename, profile, importedAt, content, List.of(), profile.equalsIgnoreCase("backup") ? ProviderType.JSON_BACKUP : ProviderType.CSV, null); }
    public static ProviderInput manual(String profile, Instant importedAt, List<ManualEntry> entries) { return new ProviderInput("manual", profile, importedAt, new byte[0], entries, ProviderType.MANUAL, null); }
    public ProviderInput withAccount(UUID id) { return new ProviderInput(filename, profile, importedAt, content, entries, type, id); }
}
