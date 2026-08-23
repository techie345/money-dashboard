package com.techie345.moneys.imports.provider;
import com.techie345.moneys.financial.*;
import java.util.*;
public class ManualEntryProvider implements FinancialDataProvider {
    private static String issueValue(String value){return value.length()<=256?value:value.substring(0,256);}
    @Override public ProviderType type(){return ProviderType.MANUAL;}
    @Override public ProviderMetadata metadata(){return new ProviderMetadata("manual","manual",null,"manual");}
    @Override public ProviderPreview preview(ProviderInput input, ProviderContext context){return new ProviderPreview(metadataFor(input,context), normalize(input,context).candidates(),List.of(),GenericCsvProvider.duplicates(normalize(input,context).candidates()));}
    @Override public NormalizedImport normalize(ProviderInput input, ProviderContext context){List<NormalizedCandidate> out=new ArrayList<>();List<ImportIssue> issues=new ArrayList<>();int row=0;for(ManualEntry e:input.entries()){row++;if(e.date()==null||e.merchant()==null||e.merchant().isBlank()||e.amountCents()<=0){issues.add(new ImportIssue(row,"invalid manual entry",issueValue(e.toString())));continue;}String text=(e.merchant()+" "+e.description()).toLowerCase(Locale.ROOT);TransactionKind kind=text.contains("transfer")?TransactionKind.TRANSFER:text.contains("payment")?TransactionKind.CREDIT_CARD_PAYMENT:TransactionKind.SPENDING;TransferType transfer=kind==TransactionKind.TRANSFER?TransferType.ACCOUNT_TRANSFER:kind==TransactionKind.CREDIT_CARD_PAYMENT?TransferType.CREDIT_CARD_PAYMENT:null;out.add(new NormalizedCandidate(row,context.accountId(),e.date(),e.merchant(),e.description()==null?e.merchant():e.description(),e.amountCents(),kind,e.category()==null?"Uncategorized":e.category(),CategorySource.MANUAL,e.sourceId(),null,transfer,ReviewStatus.NEEDS_REVIEW,null,null,null,null));}return new NormalizedImport(metadataFor(input,context),out,issues,GenericCsvProvider.duplicates(out));}
    private ProviderMetadata metadataFor(ProviderInput i,ProviderContext c){return new ProviderMetadata(i.filename(),i.profile(),i.importedAt()==null?c.now():i.importedAt(),"manual");}
}
