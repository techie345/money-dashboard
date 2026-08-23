package com.techie345.moneys.imports.provider;
import java.util.List;
import java.util.Map;
import com.techie345.moneys.financial.ReviewStatus;
public record ProviderPreview(ProviderMetadata metadata, List<NormalizedCandidate> candidates, List<ImportIssue> malformedRows, List<DuplicateCandidate> duplicates, Map<String,Long> categorization, List<NormalizedCandidate> transfers, Map<Integer,ReviewStatus> reviewInfo) {
    public ProviderPreview(ProviderMetadata m,List<NormalizedCandidate> c,List<ImportIssue> i,List<DuplicateCandidate> d){this(m,c,i,d,counts(c),c.stream().filter(x->x.transferType()!=null||x.kind().name().contains("TRANSFER")||x.kind().name().contains("PAYMENT")).toList(),reviews(c));}
    private static Map<String,Long> counts(List<NormalizedCandidate> c){return c.stream().collect(java.util.stream.Collectors.groupingBy(NormalizedCandidate::category,java.util.LinkedHashMap::new,java.util.stream.Collectors.counting()));}
    private static Map<Integer,ReviewStatus> reviews(List<NormalizedCandidate> c){return c.stream().collect(java.util.stream.Collectors.toMap(NormalizedCandidate::rowNumber,NormalizedCandidate::reviewStatus,(a,b)->b,java.util.LinkedHashMap::new));}
}
