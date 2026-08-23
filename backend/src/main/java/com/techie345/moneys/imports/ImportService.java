package com.techie345.moneys.imports;

import com.techie345.moneys.financial.*;
import com.techie345.moneys.audit.*;
import com.techie345.moneys.financial.account.AccountRepository;
import com.techie345.moneys.financial.rule.RuleRepository;
import com.techie345.moneys.financial.transaction.*;
import com.techie345.moneys.imports.provider.*;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="app.database.enabled", havingValue="true", matchIfMissing=true)
public class ImportService {
    private final ImportRepository imports; private final ImportAuditRepository audits; private final TransactionRepository transactions;
    private final RuleRepository rules; private final AccountRepository accounts; private final ProviderRegistry registry; private final ObjectMapper mapper; private final AuditService audit;
    public ImportService(ImportRepository imports, ImportAuditRepository audits, TransactionRepository transactions, RuleRepository rules, AccountRepository accounts, ProviderRegistry registry, ObjectMapper mapper) {
        this(imports, audits, transactions, rules, accounts, registry, mapper, null);
    }
    @org.springframework.beans.factory.annotation.Autowired
    public ImportService(ImportRepository imports, ImportAuditRepository audits, TransactionRepository transactions, RuleRepository rules, AccountRepository accounts, ProviderRegistry registry, ObjectMapper mapper, AuditService audit) {
        this.imports=imports; this.audits=audits; this.transactions=transactions; this.rules=rules; this.accounts=accounts; this.registry=registry; this.mapper=mapper; this.audit=audit;
    }
    @Transactional public ImportResponse create(ProviderInput input, UUID owner) {
        try { List<MerchantRuleRecord> merchantRules=rules.findAllByOwnerId(owner).stream().map(r->new MerchantRuleRecord(r.getPattern(),r.getCategory(),r.getAccountId(),r.getKind())).toList();
            ProviderContext context=new ProviderContext(owner,input.accountId(),merchantRules,Instant.now()); FinancialDataProvider provider=input.type()==ProviderType.MANUAL?registry.forType(ProviderType.MANUAL):registry.forProfile(input.profile());
            NormalizedImport normalized=prepare(provider.normalize(input,context),merchantRules,owner); String token=UUID.randomUUID().toString();
            ImportEntity e=imports.save(new ImportEntity(owner,normalized.metadata().filename(),normalized.metadata().profile(),normalized.metadata().importedAt(),token,mapper.writeValueAsString(normalized))); if (audit != null) audit.recordEntity(e, ChangeOperation.CREATED, e.getId(), Map.of("source", normalized.metadata().source())); return response(e,normalized);
        } catch(UnsupportedProviderException ex){throw new ImportFailureException("IMPORT_PROVIDER_UNSUPPORTED","The import provider is not supported.",ex);} catch(Exception ex){if(ex instanceof ImportFailureException failure)throw failure;throw new ImportFailureException("IMPORT_STAGING_FAILED","could not stage import",ex);}
    }
    @Transactional(readOnly=true) public ImportResponse preview(UUID id,UUID owner) { ImportEntity e=find(id,owner); if(!"COMMITTED".equals(e.getStatus())&&expired(e))throw new ImportConfirmationException("IMPORT_EXPIRED"); try{return response(e,mapper.readValue(e.getNormalizedJson(),NormalizedImport.class));}catch(Exception ex){throw new ImportFailureException("IMPORT_PREVIEW_UNREADABLE","could not read preview",ex);} }
    @Transactional public ImportResponse commit(UUID id,UUID owner,String token,long expectedVersion) {
        ImportEntity e=find(id,owner); try { NormalizedImport n=mapper.readValue(e.getNormalizedJson(),NormalizedImport.class); if("COMMITTED".equals(e.getStatus()))return response(e,n); if(expired(e))throw new ImportConfirmationException("IMPORT_EXPIRED");
            if(!"PREVIEW".equals(e.getStatus()))throw new ImportConfirmationException("IMPORT_NOT_COMMITTABLE"); if(!e.getConfirmationToken().equals(token))throw new ImportConfirmationException("IMPORT_CONFIRMATION_INVALID");
            if(!n.issues().isEmpty())throw new ImportConfirmationException("IMPORT_VALIDATION_FAILED"); if(!n.duplicates().isEmpty())throw new ImportConfirmationException("IMPORT_DUPLICATES_FOUND");
            for(NormalizedCandidate c:n.candidates()){if(c.accountId()==null||accounts.findByIdAndOwnerId(c.accountId(),owner).isEmpty())throw new ImportConfirmationException("IMPORT_ACCOUNT_REQUIRED"); if(transactions.findAllByOwnerId(owner).stream().anyMatch(t->sameTransaction(t,c,n.metadata().source())))throw new ImportConfirmationException("IMPORT_DUPLICATES_FOUND");}
            if(imports.claimForCommit(id,owner,expectedVersion)!=1){ImportEntity current=find(id,owner);if("COMMITTED".equals(current.getStatus()))return response(current,n);throw new ImportConfirmationException("IMPORT_VERSION_CONFLICT");}
            ImportEntity committed=find(id,owner); persist(committed,n,owner); if (audit != null) audit.recordEntity(committed, ChangeOperation.UPDATED, id, Map.of("source", n.metadata().source())); return response(committed,n);
        } catch(ImportConfirmationException ex){throw ex;} catch(Exception ex){throw new ImportFailureException("IMPORT_COMMIT_FAILED","could not commit import",ex);}
    }
    private void persist(ImportEntity committed, NormalizedImport n, UUID owner) {
        try { for(NormalizedCandidate c:n.candidates()){TransactionEntity t=new TransactionEntity(owner,c.accountId(),c.date(),c.merchant(),c.description(),c.amountCents(),c.kind(),c.category(),c.categorySource(),n.metadata().source(),n.metadata().filename(),c.sourceId()); t.updateMetadata(c.reviewStatus(),c.direction(),c.transferType(),n.metadata().importedAt(),c.action(),c.symbol(),c.shares(),c.priceCents()); transactions.saveAndFlush(t); if (audit != null) audit.recordEntity(t, ChangeOperation.CREATED, committed.getId());}
            audits.save(new ImportAuditEntity(committed.getId(),owner,"COMMIT",committed.getFilename(),committed.getProfile(),committed.getImportedAt(),n.candidates().stream().map(NormalizedCandidate::sourceId).filter(Objects::nonNull).reduce((a,b)->a+","+b).orElse("")));
        } catch(org.springframework.dao.DataIntegrityViolationException ex){throw new ImportConfirmationException("IMPORT_DUPLICATES_FOUND");}
    }
    @Transactional public void discard(UUID id,UUID owner){ImportEntity e=find(id,owner);if("COMMITTED".equals(e.getStatus()))throw new ImportConfirmationException("IMPORT_ALREADY_COMMITTED");e.discard();imports.saveAndFlush(e);if (audit != null) audit.recordEntity(e, ChangeOperation.DELETED, id);}
    private NormalizedImport prepare(NormalizedImport n,List<MerchantRuleRecord> merchantRules,UUID owner){List<NormalizedCandidate> categorized=n.candidates().stream().map(c->{if(c.accountId()==null)return c;TransactionRecord t=new TransactionRecord(null,c.date(),c.merchant(),c.description(),c.amountCents(),c.kind(),c.category(),c.accountId(),c.categorySource());TransactionRecord result=MerchantRules.apply(List.of(t),merchantRules).getFirst();return c.withCategory(result.category(),result.categorySource());}).toList();List<DuplicateCandidate> duplicates=new ArrayList<>(n.duplicates());for(TransactionEntity t:Optional.ofNullable(transactions.findAllByOwnerId(owner)).orElse(List.of()))for(NormalizedCandidate c:categorized)if(sameTransaction(t,c,n.metadata().source()))duplicates.add(new DuplicateCandidate(c.rowNumber(),0,"matches existing canonical transaction"));return new NormalizedImport(n.metadata(),categorized,n.issues(),duplicates);}
    private boolean sameTransaction(TransactionEntity t,NormalizedCandidate c,String source){return (c.sourceId()!=null&&c.sourceId().equals(t.getSourceId())&&source.equals(t.getSource())&&Objects.equals(c.accountId(),t.getAccountId()))||(Objects.equals(c.accountId(),t.getAccountId())&&c.date().equals(t.getDate())&&c.merchant().equalsIgnoreCase(t.getMerchant())&&c.amountCents()==t.getAmountCents()&&c.kind()==t.getKind());}
    private boolean expired(ImportEntity e){return e.getExpiresAt()!=null&&e.getExpiresAt().isBefore(Instant.now());}
    private ImportEntity find(UUID id,UUID owner){return imports.findByIdAndOwnerId(id,owner).orElseThrow(ImportNotFoundException::new);} private ImportResponse response(ImportEntity e){return new ImportResponse(e.getId(),e.getConfirmationToken(),e.getVersion(),e.getStatus());} private ImportResponse response(ImportEntity e,NormalizedImport n){return new ImportResponse(e.getId(),e.getConfirmationToken(),e.getVersion(),e.getStatus(),new ProviderPreview(n.metadata(),n.candidates(),n.issues(),n.duplicates()));}
}
