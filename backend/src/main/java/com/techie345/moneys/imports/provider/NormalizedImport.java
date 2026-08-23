package com.techie345.moneys.imports.provider;
import java.util.List;
public record NormalizedImport(ProviderMetadata metadata, List<NormalizedCandidate> candidates, List<ImportIssue> issues, List<DuplicateCandidate> duplicates) { }
