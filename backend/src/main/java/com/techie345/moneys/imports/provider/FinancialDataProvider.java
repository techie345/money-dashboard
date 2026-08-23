package com.techie345.moneys.imports.provider;
public interface FinancialDataProvider { ProviderType type(); ProviderMetadata metadata(); ProviderPreview preview(ProviderInput input, ProviderContext context); NormalizedImport normalize(ProviderInput input, ProviderContext context); }
