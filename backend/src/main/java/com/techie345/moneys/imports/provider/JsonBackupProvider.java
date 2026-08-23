package com.techie345.moneys.imports.provider;
import tools.jackson.databind.ObjectMapper;
import com.techie345.moneys.financial.*;
import java.time.LocalDate;
import java.util.*;
public class JsonBackupProvider extends AbstractFileProvider {
    private final ObjectMapper mapper = new ObjectMapper();
    @Override public ProviderType type() { return ProviderType.JSON_BACKUP; }
    @Override public ProviderMetadata metadata() { return new ProviderMetadata("", "backup", null, "json_backup"); }
    @Override public ProviderPreview preview(ProviderInput input, ProviderContext context) { return previewOf(normalize(input, context)); }
    @Override public NormalizedImport normalize(ProviderInput original, ProviderContext context) {
        ProviderInput input = new ProviderInput(original.filename(), original.profile(), importedAt(original, context), original.content(), original.entries(), original.type(), original.accountId());
        List<NormalizedCandidate> rows=new ArrayList<>(); List<ImportIssue> issues=new ArrayList<>();
        try { List<Map<String,Object>> values=mapper.readValue(input.content(), List.class); int row=0; for(var value:values){row++; try {
            LocalDate date=LocalDate.parse(String.valueOf(value.get("date"))); String merchant=String.valueOf(value.getOrDefault("merchant", value.get("description"))); String description=String.valueOf(value.getOrDefault("description", merchant));
            long cents=money(value.get("amountCents"), "amountCents"); if(cents<=0)throw new IllegalArgumentException("amountCents must be positive"); String category=String.valueOf(value.getOrDefault("category","Uncategorized"));
            CategorySource source=CategorySource.valueOf(String.valueOf(value.getOrDefault("categorySource","IMPORTED")).toUpperCase(Locale.ROOT));
            String explicitKind=value.get("kind")==null?null:String.valueOf(value.get("kind"));
            TransactionKind kind=classifyKind(explicitKind, merchant+" "+description, false, value.containsKey("symbol")||value.containsKey("shares")||value.containsKey("priceCents"));
            String explicitTransfer=value.get("transferType")==null?null:String.valueOf(value.get("transferType"));
            TransferType transfer=transferType(explicitTransfer,kind);
            TransactionDirection direction=kind==TransactionKind.SPENDING||kind==TransactionKind.TRANSFER||kind==TransactionKind.CREDIT_CARD_PAYMENT||kind==TransactionKind.INVESTMENT?TransactionDirection.OUTFLOW:TransactionDirection.INFLOW;
            java.math.BigDecimal shares=value.get("shares")==null?null:new java.math.BigDecimal(String.valueOf(value.get("shares")));
            Long price=value.get("priceCents")==null?null:money(value.get("priceCents"), "priceCents");
            rows.add(new NormalizedCandidate(row,context.accountId(),date,merchant,description,cents,kind,category,source,(String)value.get("sourceId"),direction,transfer,ReviewStatus.valueOf(String.valueOf(value.getOrDefault("reviewStatus","NEEDS_REVIEW")).toUpperCase(Locale.ROOT)),(String)value.get("action"),(String)value.get("symbol"),shares,price));
        }catch(Exception e){issues.add(new ImportIssue(row,"malformed row: "+e.getMessage(),issueValue(value.toString())));}} } catch(Exception e){issues.add(new ImportIssue(1,"malformed JSON: "+e.getMessage(),issueValue(text(input.content()))));}
        return new NormalizedImport(metadataFor(input),rows,issues,GenericCsvProvider.duplicates(rows));
    }
    private static long money(Object value, String name) {
        if (value == null) throw new IllegalArgumentException(name + " is required");
        try { return new java.math.BigDecimal(String.valueOf(value)).toBigIntegerExact().longValueExact(); }
        catch (ArithmeticException | NumberFormatException e) { throw new IllegalArgumentException(name + " must be an integer in long range"); }
    }
}
