package com.techie345.moneys.imports.provider;
import com.techie345.moneys.financial.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
public class GenericCsvProvider extends AbstractFileProvider {
    private ProviderInput timestamped(ProviderInput i, ProviderContext c) { return new ProviderInput(i.filename(), i.profile(), importedAt(i, c), i.content(), i.entries(), i.type(), i.accountId()); }
    @Override public ProviderType type() { return ProviderType.CSV; }
    @Override public ProviderMetadata metadata() { return new ProviderMetadata("", "generic", null, "csv"); }
    @Override public ProviderPreview preview(ProviderInput input, ProviderContext context) { return previewOf(normalize(input, context)); }
    @Override public NormalizedImport normalize(ProviderInput original, ProviderContext context) {
        ProviderInput input = timestamped(original, context); List<NormalizedCandidate> rows = new ArrayList<>(); List<ImportIssue> issues = new ArrayList<>();
        List<CsvRecord> records = parseRecords(text(input.content()));
        if (records.isEmpty() || records.getFirst().values().stream().allMatch(String::isBlank)) return new NormalizedImport(metadataFor(input), rows, List.of(new ImportIssue(1, "missing header", "")), List.of());
        List<String> headers = records.getFirst().values().stream().map(s -> MerchantRules.normalize(s).replace(" ", "")).toList();
        for (int index = 1; index < records.size(); index++) { CsvRecord record = records.get(index); List<String> v = record.values(); if (v.size() == 1 && v.getFirst().isBlank()) continue;
            try { if (v.size() != headers.size()) throw new IllegalArgumentException("wrong column count"); String date = value(headers, v, "date"); String desc = value(headers, v, "description", "merchant", "name"); String amount = value(headers, v, "amount", "amountcents"); String category = optional(headers, v, "category").orElse("Uncategorized"); String sourceId = optional(headers, v, "transactionid", "sourceid", "id").orElse(null);
                boolean negative = amount.trim().startsWith("-"); long cents = amount.contains(".") ? new BigDecimal(amount).movePointRight(2).longValueExact() : Long.parseLong(amount); cents = Math.abs(cents); if (cents <= 0) throw new IllegalArgumentException("amount must be non-zero");
                CategorySource categorySource = optional(headers, v, "categorysource").map(s -> CategorySource.valueOf(s.toUpperCase(Locale.ROOT))).orElse(CategorySource.IMPORTED);
                String text = desc + " " + optional(headers, v, "kind", "transfertype").orElse("");
                TransactionKind kind = classifyKind(optional(headers, v, "kind").orElse(null), text, negative, optional(headers, v, "symbol", "shares", "pricecents").isPresent());
                rows.add(new NormalizedCandidate(record.startLine(), context.accountId(), LocalDate.parse(date), desc, desc, cents, kind, category, categorySource, sourceId, direction(negative), transferType(optional(headers, v, "transfertype").orElse(null), kind), ReviewStatus.NEEDS_REVIEW, optional(headers, v, "action").orElse(null), optional(headers, v, "symbol").orElse(null), null, null));
            } catch (Exception e) { issues.add(new ImportIssue(record.startLine(), "malformed row: " + e.getMessage(), issueValue(record.raw()))); }
        }
        return new NormalizedImport(metadataFor(input), rows, issues, duplicates(rows));
    }
    private static Optional<String> optional(List<String> h, List<String> v, String... names) { for (String n : names) { int i=h.indexOf(n); if(i>=0&&i<v.size()&&!v.get(i).isBlank()) return Optional.of(v.get(i).trim()); } return Optional.empty(); }
    private static String value(List<String> h, List<String> v, String... names) { return optional(h,v,names).orElseThrow(() -> new IllegalArgumentException("missing " + names[0])); }
    static List<CsvRecord> parseRecords(String content) { List<CsvRecord> out=new ArrayList<>(); List<String> fields=new ArrayList<>(); StringBuilder field=new StringBuilder(); StringBuilder raw=new StringBuilder(); boolean quoted=false; int line=1, start=1; for(int i=0;i<content.length();i++){char c=content.charAt(i); raw.append(c); if(c=='"'){if(quoted&&i+1<content.length()&&content.charAt(i+1)=='"'){field.append('"');raw.append(content.charAt(++i));}else quoted=!quoted;} else if(c==','&&!quoted){fields.add(field.toString().trim());field.setLength(0);} else if((c=='\n'||c=='\r')&&!quoted){if(c=='\r'&&i+1<content.length()&&content.charAt(i+1)=='\n'){raw.append(content.charAt(++i));}fields.add(field.toString().trim());field.setLength(0);out.add(new CsvRecord(start,List.copyOf(fields),raw.toString()));fields.clear();raw.setLength(0);line++;start=line;} else {field.append(c); if(c=='\n')line++;}} if(quoted){out.add(new CsvRecord(start,List.of(),raw.toString()));} else if(field.length()>0||!fields.isEmpty()){fields.add(field.toString().trim());out.add(new CsvRecord(start,List.copyOf(fields),raw.toString()));} return out; }
    record CsvRecord(int startLine,List<String> values,String raw) { }
    static List<DuplicateCandidate> duplicates(List<NormalizedCandidate> rows) { List<DuplicateCandidate> out=new ArrayList<>(); for(int i=0;i<rows.size();i++)for(int j=0;j<i;j++){var a=rows.get(i);var b=rows.get(j);if(sourceIdentityMatches(a,b)||valueIdentityMatches(a,b))out.add(new DuplicateCandidate(a.rowNumber(),b.rowNumber(),a.sourceId()!=null&&b.sourceId()!=null?"duplicate source identifier":"same account, date, merchant, amount, and kind"));}return out; }
    static boolean sourceIdentityMatches(NormalizedCandidate a, NormalizedCandidate b) { return a.sourceId()!=null&&b.sourceId()!=null&&Objects.equals(a.accountId(),b.accountId())&&a.sourceId().equals(b.sourceId()); }
    static boolean valueIdentityMatches(NormalizedCandidate a, NormalizedCandidate b) { return Objects.equals(a.accountId(),b.accountId())&&a.date().equals(b.date())&&a.amountCents()==b.amountCents()&&MerchantRules.normalize(a.merchant()).equals(MerchantRules.normalize(b.merchant()))&&a.kind()==b.kind(); }
}
