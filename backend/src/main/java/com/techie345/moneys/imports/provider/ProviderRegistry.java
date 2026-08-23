package com.techie345.moneys.imports.provider;
import java.util.*;
import org.springframework.stereotype.Component;
@Component public class ProviderRegistry {
    private final Map<ProviderType,FinancialDataProvider> providers;
    public ProviderRegistry(List<FinancialDataProvider> values){providers=new EnumMap<>(ProviderType.class);values.forEach(v->providers.put(v.type(),v));}
    public FinancialDataProvider get(ProviderType type){var p=providers.get(type);if(p==null)throw new UnsupportedProviderException(type);return p;}
    public FinancialDataProvider forType(ProviderType type){return get(type);}
    public FinancialDataProvider forProfile(String profile){return get("backup".equalsIgnoreCase(profile)?ProviderType.JSON_BACKUP:ProviderType.CSV);}
}
