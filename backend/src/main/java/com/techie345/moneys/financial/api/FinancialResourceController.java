package com.techie345.moneys.financial.api;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.techie345.moneys.financial.account.AccountEntity;
import com.techie345.moneys.financial.account.AccountRepository;
import com.techie345.moneys.financial.api.FinancialResourceDtos.AccountRequest;
import com.techie345.moneys.financial.api.FinancialResourceDtos.AccountResponse;
import com.techie345.moneys.financial.api.FinancialResourceDtos.TransactionRequest;
import com.techie345.moneys.financial.api.FinancialResourceDtos.TransactionResponse;
import com.techie345.moneys.financial.transaction.TransactionEntity;
import com.techie345.moneys.financial.transaction.TransactionRepository;
import com.techie345.moneys.financial.asset.*;
import com.techie345.moneys.financial.holding.*;
import com.techie345.moneys.financial.liability.*;
import com.techie345.moneys.financial.obligation.*;
import com.techie345.moneys.financial.goal.*;
import com.techie345.moneys.financial.rule.*;
import com.techie345.moneys.financial.snapshot.*;
import com.techie345.moneys.financial.api.FinancialResourceDtos.*;
import com.techie345.moneys.identity.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@ConditionalOnProperty(name = "app.financial.api.enabled", havingValue = "true", matchIfMissing = true)
public class FinancialResourceController {
    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final AssetRepository assets; private final HoldingRepository holdings; private final LiabilityRepository liabilities;
    private final ObligationRepository obligations; private final GoalRepository goals; private final RuleRepository rules; private final SnapshotRepository snapshots;
    private final com.techie345.moneys.financial.FinancialMutationService mutations;
    private final com.techie345.moneys.financial.FinancialCalculationService calculations;

    public FinancialResourceController(AccountRepository accounts, TransactionRepository transactions, AssetRepository assets, HoldingRepository holdings,
            LiabilityRepository liabilities, ObligationRepository obligations, GoalRepository goals, RuleRepository rules, SnapshotRepository snapshots,
            com.techie345.moneys.financial.FinancialMutationService mutations,
            com.techie345.moneys.financial.FinancialCalculationService calculations) {
        this.accounts=accounts; this.transactions=transactions; this.assets=assets; this.holdings=holdings; this.liabilities=liabilities;
        this.obligations=obligations; this.goals=goals; this.rules=rules; this.snapshots=snapshots;
        this.mutations=mutations;
        this.calculations = calculations;
    }

    @PostMapping("/accounts")
    public ResponseEntity<AccountResponse> createAccount(
            CurrentUser currentUser,
            @Valid @RequestBody AccountRequest request) {
        UUID ownerId = currentUser.id();
        AccountEntity saved = mutations.create(accounts, new AccountEntity(ownerId, request.institution(), request.name(),
                request.kind(), request.balanceCents()), e -> e.updateMetadata(request.lastImportedAt(), request.source(), request.sourceFile(), request.importedAt()));
        return ResponseEntity.created(URI.create("/api/v1/accounts/" + saved.getId())).body(response(saved));
    }

    @PostMapping("/transactions")
    public ResponseEntity<TransactionResponse> createTransaction(
            CurrentUser currentUser,
            @Valid @RequestBody TransactionRequest request) {
        UUID ownerId = currentUser.id();
        if (accounts.findByIdAndOwnerId(request.accountId(), ownerId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        TransactionEntity saved = mutations.create(transactions, new TransactionEntity(ownerId, request.accountId(), request.date(),
                request.merchant(), request.description(), request.amountCents(), request.kind(), request.category(),
                request.categorySource(), request.source(), request.sourceFile(), request.sourceId()), e -> e.updateMetadata(request.reviewStatus(), request.direction(), request.transferType(), request.importedAt(), request.action(), request.symbol(), request.shares(), request.priceCents()));
        return ResponseEntity.created(URI.create("/api/v1/transactions/" + saved.getId())).body(transactionResponse(saved));
    }

    @PatchMapping("/transactions/{id}")
    public ResponseEntity<TransactionResponse> updateTransaction(CurrentUser user, @PathVariable UUID id, @RequestHeader("If-Match") String version, @Valid @RequestBody TransactionRequest r) {
        if (accounts.findByIdAndOwnerId(r.accountId(), user.id()).isEmpty()) return ResponseEntity.notFound().build();
        TransactionEntity updated = transactions.findByIdAndOwnerId(id,user.id()).map(t -> {
            check(t.getVersion(),parseVersion(version)); t.update(r.date(),r.merchant(),r.description(),r.amountCents(),r.kind(),r.category(),r.accountId(),r.categorySource(),r.source(),r.sourceFile(),r.sourceId());
            t.updateMetadata(r.reviewStatus(),r.direction(),r.transferType(),r.importedAt(),r.action(),r.symbol(),r.shares(),r.priceCents()); return mutations.create(transactions,t);
        }).orElse(null);
        return updated == null ? ResponseEntity.notFound().build() : ResponseEntity.ok().eTag(etag(updated.getVersion())).body(transactionResponse(updated));
    }

    @PostMapping("/assets") public ResponseEntity<AssetResponse> createAsset(CurrentUser u,@Valid @RequestBody AssetRequest r){AssetEntity e=mutations.create(assets,new AssetEntity(u.id(),r.name(),r.kind(),r.valueCents()));return ResponseEntity.created(uri("assets",e.getId())).body(asset(e));}
    @GetMapping("/assets") public List<AssetResponse> listAssets(CurrentUser u){return assets.findAllByOwnerId(u.id()).stream().map(FinancialResourceController::asset).toList();}
    @GetMapping("/assets/{id}") public ResponseEntity<AssetResponse> getAsset(CurrentUser u,@PathVariable UUID id){return assets.findByIdAndOwnerId(id,u.id()).map(e->ResponseEntity.ok(asset(e))).orElseGet(()->ResponseEntity.notFound().build());}
    @PatchMapping("/assets/{id}") public ResponseEntity<AssetResponse> updateAsset(CurrentUser u,@PathVariable UUID id,@RequestHeader("If-Match") String v,@Valid @RequestBody AssetRequest r){AssetEntity e=mutations.update(()->assets.findByIdAndOwnerId(id,u.id()).orElse(null),parseVersion(v),x->x.update(r.name(),r.kind(),r.valueCents()),assets);return e==null?ResponseEntity.notFound().build():ResponseEntity.ok().eTag(etag(e.getVersion())).body(asset(e));}
    @DeleteMapping("/assets/{id}") public ResponseEntity<Void> deleteAsset(CurrentUser u,@PathVariable UUID id,@RequestHeader("If-Match") String v){return delete(()->assets.findByIdAndOwnerId(id,u.id()).orElse(null),parseVersion(v),AssetEntity::getVersion,assets);}

    @PostMapping("/investment-holdings") public ResponseEntity<HoldingResponse> createHolding(CurrentUser u,@Valid @RequestBody HoldingRequest r){if(accounts.findByIdAndOwnerId(r.accountId(),u.id()).isEmpty())return ResponseEntity.notFound().build();HoldingEntity e=mutations.create(holdings,new HoldingEntity(u.id(),r.accountId(),r.symbol(),r.shares(),r.costBasisCents(),r.marketValueCents(),r.source(),r.sourceFile(),r.importedAt()));return ResponseEntity.created(uri("investment-holdings",e.getId())).body(holding(e));}
    @GetMapping("/investment-holdings") public List<HoldingResponse> listHoldings(CurrentUser u){return holdings.findAllByOwnerId(u.id()).stream().map(FinancialResourceController::holding).toList();}
    @GetMapping("/investment-holdings/{id}") public ResponseEntity<HoldingResponse> getHolding(CurrentUser u,@PathVariable UUID id){return holdings.findByIdAndOwnerId(id,u.id()).map(e->ResponseEntity.ok(holding(e))).orElseGet(()->ResponseEntity.notFound().build());}
    @PatchMapping("/investment-holdings/{id}") public ResponseEntity<HoldingResponse> updateHolding(CurrentUser u,@PathVariable UUID id,@RequestHeader("If-Match") String v,@Valid @RequestBody HoldingRequest r){if(accounts.findByIdAndOwnerId(r.accountId(),u.id()).isEmpty())return ResponseEntity.notFound().build();HoldingEntity e=mutations.update(()->holdings.findByIdAndOwnerId(id,u.id()).orElse(null),parseVersion(v),x->x.update(r.accountId(),r.symbol(),r.shares(),r.costBasisCents(),r.marketValueCents(),r.source(),r.sourceFile(),r.importedAt()),holdings);return e==null?ResponseEntity.notFound().build():ResponseEntity.ok().eTag(etag(e.getVersion())).body(holding(e));}
    @DeleteMapping("/investment-holdings/{id}") public ResponseEntity<Void> deleteHolding(CurrentUser u,@PathVariable UUID id,@RequestHeader("If-Match") String v){return delete(()->holdings.findByIdAndOwnerId(id,u.id()).orElse(null),parseVersion(v),HoldingEntity::getVersion,holdings);}

    @PostMapping("/liabilities") public ResponseEntity<LiabilityResponse> createLiability(CurrentUser u,@Valid @RequestBody LiabilityRequest r){if(r.accountId()!=null&&accounts.findByIdAndOwnerId(r.accountId(),u.id()).isEmpty())return ResponseEntity.notFound().build();LiabilityEntity e=mutations.create(liabilities,new LiabilityEntity(u.id(),r.name(),r.kind(),r.balanceCents(),r.interestRate(),r.minimumPaymentCents(),r.accountId(),r.source(),r.sourceFile(),r.importedAt()));return ResponseEntity.created(uri("liabilities",e.getId())).body(liability(e));}
    @GetMapping("/liabilities") public List<LiabilityResponse> listLiabilities(CurrentUser u){return liabilities.findAllByOwnerId(u.id()).stream().map(FinancialResourceController::liability).toList();}
    @GetMapping("/liabilities/{id}") public ResponseEntity<LiabilityResponse> getLiability(CurrentUser u,@PathVariable UUID id){return liabilities.findByIdAndOwnerId(id,u.id()).map(e->ResponseEntity.ok(liability(e))).orElseGet(()->ResponseEntity.notFound().build());}
    @PatchMapping("/liabilities/{id}") public ResponseEntity<LiabilityResponse> updateLiability(CurrentUser u,@PathVariable UUID id,@RequestHeader("If-Match") String v,@Valid @RequestBody LiabilityRequest r){if(r.accountId()!=null&&accounts.findByIdAndOwnerId(r.accountId(),u.id()).isEmpty())return ResponseEntity.notFound().build();LiabilityEntity e=mutations.update(()->liabilities.findByIdAndOwnerId(id,u.id()).orElse(null),parseVersion(v),x->x.update(r.name(),r.kind(),r.balanceCents(),r.interestRate(),r.minimumPaymentCents(),r.accountId(),r.source(),r.sourceFile(),r.importedAt()),liabilities);return e==null?ResponseEntity.notFound().build():ResponseEntity.ok().eTag(etag(e.getVersion())).body(liability(e));}
    @DeleteMapping("/liabilities/{id}") public ResponseEntity<Void> deleteLiability(CurrentUser u,@PathVariable UUID id,@RequestHeader("If-Match") String v){return delete(()->liabilities.findByIdAndOwnerId(id,u.id()).orElse(null),parseVersion(v),LiabilityEntity::getVersion,liabilities);}

    @PostMapping("/recurring-obligations") public ResponseEntity<ObligationResponse> createObligation(CurrentUser u,@Valid @RequestBody ObligationRequest r){ObligationEntity e=mutations.create(obligations,new ObligationEntity(u.id(),r.name(),r.category(),r.amountCents(),r.frequency(),r.nextDueDate()));return ResponseEntity.created(uri("recurring-obligations",e.getId())).body(obligation(e));}
    @GetMapping("/recurring-obligations") public List<ObligationResponse> listObligations(CurrentUser u){return obligations.findAllByOwnerId(u.id()).stream().map(FinancialResourceController::obligation).toList();}
    @GetMapping("/recurring-obligations/{id}") public ResponseEntity<ObligationResponse> getObligation(CurrentUser u,@PathVariable UUID id){return obligations.findByIdAndOwnerId(id,u.id()).map(e->ResponseEntity.ok(obligation(e))).orElseGet(()->ResponseEntity.notFound().build());}
    @PatchMapping("/recurring-obligations/{id}") public ResponseEntity<ObligationResponse> updateObligation(CurrentUser u,@PathVariable UUID id,@RequestHeader("If-Match") String v,@Valid @RequestBody ObligationRequest r){ObligationEntity e=mutations.update(()->obligations.findByIdAndOwnerId(id,u.id()).orElse(null),parseVersion(v),x->x.update(r.name(),r.category(),r.amountCents(),r.frequency(),r.nextDueDate()),obligations);return e==null?ResponseEntity.notFound().build():ResponseEntity.ok().eTag(etag(e.getVersion())).body(obligation(e));}
    @DeleteMapping("/recurring-obligations/{id}") public ResponseEntity<Void> deleteObligation(CurrentUser u,@PathVariable UUID id,@RequestHeader("If-Match") String v){return delete(()->obligations.findByIdAndOwnerId(id,u.id()).orElse(null),parseVersion(v),ObligationEntity::getVersion,obligations);}
    @PostMapping("/savings-goals") public ResponseEntity<GoalResponse> createGoal(CurrentUser u,@Valid @RequestBody GoalRequest r){GoalEntity e=mutations.create(goals,new GoalEntity(u.id(),r.name(),r.targetCents(),r.currentCents(),r.deadline()));return ResponseEntity.created(uri("savings-goals",e.getId())).body(goal(e));}
    @GetMapping("/savings-goals") public List<GoalResponse> listGoals(CurrentUser u){return goals.findAllByOwnerId(u.id()).stream().map(FinancialResourceController::goal).toList();}
    @GetMapping("/savings-goals/{id}") public ResponseEntity<GoalResponse> getGoal(CurrentUser u,@PathVariable UUID id){return goals.findByIdAndOwnerId(id,u.id()).map(e->ResponseEntity.ok(goal(e))).orElseGet(()->ResponseEntity.notFound().build());}
    @PatchMapping("/savings-goals/{id}") public ResponseEntity<GoalResponse> updateGoal(CurrentUser u,@PathVariable UUID id,@RequestHeader("If-Match") String v,@Valid @RequestBody GoalRequest r){GoalEntity e=mutations.update(()->goals.findByIdAndOwnerId(id,u.id()).orElse(null),parseVersion(v),x->x.update(r.name(),r.targetCents(),r.currentCents(),r.deadline()),goals);return e==null?ResponseEntity.notFound().build():ResponseEntity.ok().eTag(etag(e.getVersion())).body(goal(e));}
    @DeleteMapping("/savings-goals/{id}") public ResponseEntity<Void> deleteGoal(CurrentUser u,@PathVariable UUID id,@RequestHeader("If-Match") String v){return delete(()->goals.findByIdAndOwnerId(id,u.id()).orElse(null),parseVersion(v),GoalEntity::getVersion,goals);}
    @PostMapping("/merchant-rules") public ResponseEntity<RuleResponse> createRule(CurrentUser u,@Valid @RequestBody RuleRequest r){if(r.accountId()!=null&&accounts.findByIdAndOwnerId(r.accountId(),u.id()).isEmpty())return ResponseEntity.notFound().build();RuleEntity e=mutations.create(rules,new RuleEntity(u.id(),r.pattern(),r.category(),r.accountId(),r.kind()));return ResponseEntity.created(uri("merchant-rules",e.getId())).body(rule(e));}
    @GetMapping("/merchant-rules") public List<RuleResponse> listRules(CurrentUser u){return rules.findAllByOwnerId(u.id()).stream().map(FinancialResourceController::rule).toList();}
    @GetMapping("/merchant-rules/{id}") public ResponseEntity<RuleResponse> getRule(CurrentUser u,@PathVariable UUID id){return rules.findByIdAndOwnerId(id,u.id()).map(e->ResponseEntity.ok(rule(e))).orElseGet(()->ResponseEntity.notFound().build());}
    @PatchMapping("/merchant-rules/{id}") public ResponseEntity<RuleResponse> updateRule(CurrentUser u,@PathVariable UUID id,@RequestHeader("If-Match") String v,@Valid @RequestBody RuleRequest r){if(r.accountId()!=null&&accounts.findByIdAndOwnerId(r.accountId(),u.id()).isEmpty())return ResponseEntity.notFound().build();RuleEntity e=mutations.update(()->rules.findByIdAndOwnerId(id,u.id()).orElse(null),parseVersion(v),x->x.update(r.pattern(),r.category(),r.accountId(),r.kind()),rules);return e==null?ResponseEntity.notFound().build():ResponseEntity.ok().eTag(etag(e.getVersion())).body(rule(e));}
    @DeleteMapping("/merchant-rules/{id}") public ResponseEntity<Void> deleteRule(CurrentUser u,@PathVariable UUID id,@RequestHeader("If-Match") String v){return delete(()->rules.findByIdAndOwnerId(id,u.id()).orElse(null),parseVersion(v),RuleEntity::getVersion,rules);}
    @GetMapping("/net-worth-snapshots") public List<SnapshotResponse> listSnapshots(CurrentUser u){return snapshots.findAllByOwnerId(u.id()).stream().map(FinancialResourceController::snapshot).toList();}
    @GetMapping("/net-worth-snapshots/{id}") public ResponseEntity<SnapshotResponse> getSnapshot(CurrentUser u,@PathVariable UUID id){return snapshots.findByIdAndOwnerId(id,u.id()).map(e->ResponseEntity.ok(snapshot(e))).orElseGet(()->ResponseEntity.notFound().build());}
    @PostMapping("/net-worth-snapshots") public ResponseEntity<SnapshotResponse> createSnapshot(CurrentUser u,@Valid @RequestBody SnapshotRequest r){if(r.accountId()!=null&&accounts.findByIdAndOwnerId(r.accountId(),u.id()).isEmpty())return ResponseEntity.notFound().build();SnapshotEntity e=mutations.create(snapshots,new SnapshotEntity(u.id(),r.date(),r.assetsCents(),r.liabilitiesCents(),r.netWorthCents(),r.accountId(),r.source(),r.sourceFile(),r.importedAt()));return ResponseEntity.created(uri("net-worth-snapshots",e.getId())).eTag(etag(e.getVersion())).body(snapshot(e));}
    @PatchMapping("/net-worth-snapshots/{id}") public ResponseEntity<SnapshotResponse> updateSnapshot(CurrentUser u,@PathVariable UUID id,@RequestHeader("If-Match") String v,@Valid @RequestBody SnapshotRequest r){if(r.accountId()!=null&&accounts.findByIdAndOwnerId(r.accountId(),u.id()).isEmpty())return ResponseEntity.notFound().build();SnapshotEntity e=mutations.update(()->snapshots.findByIdAndOwnerId(id,u.id()).orElse(null),parseVersion(v),x->x.update(r.date(),r.assetsCents(),r.liabilitiesCents(),r.netWorthCents(),r.accountId(),r.source(),r.sourceFile(),r.importedAt()),snapshots);return e==null?ResponseEntity.notFound().build():ResponseEntity.ok().eTag(etag(e.getVersion())).body(snapshot(e));}
    @DeleteMapping("/net-worth-snapshots/{id}") public ResponseEntity<Void> deleteSnapshot(CurrentUser u,@PathVariable UUID id,@RequestHeader("If-Match") String v){return delete(()->snapshots.findByIdAndOwnerId(id,u.id()).orElse(null),parseVersion(v),SnapshotEntity::getVersion,snapshots);}
    @GetMapping("/dashboard/overview") public com.techie345.moneys.financial.CalculationReadModels.Overview overview(CurrentUser u){return calculations.overview(u.id());}
    @GetMapping("/dashboard/spending") public com.techie345.moneys.financial.CalculationReadModels.Spending spending(CurrentUser u){return calculations.spending(u.id());}
    @GetMapping("/dashboard/income") public com.techie345.moneys.financial.CalculationReadModels.Income income(CurrentUser u){return calculations.income(u.id());}
    @GetMapping("/dashboard/investing") public com.techie345.moneys.financial.CalculationReadModels.Investing investing(CurrentUser u){return calculations.investing(u.id());}

    @GetMapping("/transactions")
    public List<TransactionResponse> listTransactions(CurrentUser currentUser) {
        UUID ownerId = currentUser.id();
        return transactions.findAllByOwnerId(ownerId).stream().map(FinancialResourceController::transactionResponse).toList();
    }

    @GetMapping("/transactions/{id}")
    public ResponseEntity<TransactionResponse> getTransaction(CurrentUser currentUser,
                                                               @PathVariable UUID id) {
        UUID ownerId = currentUser.id();
        return transactions.findByIdAndOwnerId(id, ownerId).map(transaction -> ResponseEntity.ok(transactionResponse(transaction)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/transactions/{id}")
    public ResponseEntity<Void> deleteTransaction(CurrentUser currentUser, @PathVariable UUID id, @RequestHeader("If-Match") String version) {
        UUID ownerId = currentUser.id();
        return mutations.delete(() -> transactions.findByIdAndOwnerId(id, ownerId).orElse(null), parseVersion(version), TransactionEntity::getVersion, transactions)
                ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/accounts")
    public List<AccountResponse> listAccounts(CurrentUser currentUser) {
        UUID ownerId = currentUser.id();
        return accounts.findAllByOwnerId(ownerId).stream().map(FinancialResourceController::response).toList();
    }

    @GetMapping("/accounts/{id}")
    public ResponseEntity<AccountResponse> getAccount(CurrentUser currentUser,
                                                       @PathVariable UUID id) {
        UUID ownerId = currentUser.id();
        return accounts.findByIdAndOwnerId(id, ownerId).map(account -> ResponseEntity.ok(response(account)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/accounts/{id}")
    public ResponseEntity<AccountResponse> updateAccount(CurrentUser currentUser,
                                                         @PathVariable UUID id,
                                                         @RequestHeader("If-Match") String expectedVersion,
                                                         @Valid @RequestBody AccountRequest request) {
        UUID ownerId = currentUser.id();
        return accounts.findByIdAndOwnerId(id, ownerId).map(account -> {
            if (account.getVersion() != parseVersion(expectedVersion)) throw new VersionConflictException();
            account.update(request.institution(), request.name(), request.kind(), request.balanceCents());
            account.updateMetadata(request.lastImportedAt(), request.source(), request.sourceFile(), request.importedAt());
            AccountEntity updated = mutations.create(accounts, account);
            return ResponseEntity.ok().eTag(etag(updated.getVersion())).body(response(updated));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/accounts/{id}")
    public ResponseEntity<Void> deleteAccount(CurrentUser currentUser,
                                               @PathVariable UUID id, @RequestHeader("If-Match") String version) {
        UUID ownerId = currentUser.id();
        return mutations.delete(() -> accounts.findByIdAndOwnerId(id, ownerId).orElse(null), parseVersion(version), AccountEntity::getVersion, accounts)
                ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    private static AccountResponse response(AccountEntity account) {
        return new AccountResponse(account.getId(), account.getOwnerId(), account.getInstitution(), account.getName(),
                account.getKind(), account.getBalanceCents(), account.getLastImportedAt(), account.getSource(), account.getSourceFile(), account.getImportedAt(), account.getVersion());
    }

    private static TransactionResponse transactionResponse(TransactionEntity transaction) {
        return new TransactionResponse(transaction.getId(), transaction.getOwnerId(), transaction.getAccountId(),
                transaction.getDate(), transaction.getMerchant(), transaction.getDescription(), transaction.getAmountCents(),
                transaction.getKind(), transaction.getCategory(), transaction.getCategorySource(), transaction.getSource(),
                transaction.getSourceFile(), transaction.getSourceId(), transaction.getReviewStatus(), transaction.getDirection(),
                transaction.getTransferType(), transaction.getImportedAt(), transaction.getAction(), transaction.getSymbol(),
                transaction.getShares(), transaction.getPriceCents(), transaction.getVersion());
    }
    private static URI uri(String path,UUID id){return URI.create("/api/v1/"+path+"/"+id);}
    private static String etag(long version){return "\""+version+"\"";}
    static long parseVersion(String value) {
        String normalized = value.trim();
        if (normalized.startsWith("W/")) throw new IllegalArgumentException("weak If-Match validators are not supported");
        if (normalized.length() >= 2 && normalized.startsWith("\"") && normalized.endsWith("\"")) normalized = normalized.substring(1, normalized.length() - 1);
        try { return Long.parseLong(normalized); } catch (NumberFormatException ex) { throw new IllegalArgumentException("If-Match must contain a resource version"); }
    }
    private static void check(long actual,long expected){if(actual!=expected)throw new VersionConflictException();}
    private <T> ResponseEntity<Void> delete(java.util.function.Supplier<T> lookup,long version,java.util.function.ToLongFunction<T> actual,org.springframework.data.jpa.repository.JpaRepository<T,UUID> repo){return mutations.delete(lookup,version,actual,repo)?ResponseEntity.noContent().build():ResponseEntity.notFound().build();}
    private static AssetResponse asset(AssetEntity e){return new AssetResponse(e.getId(),e.getOwnerId(),e.getName(),e.getKind(),e.getValueCents(),e.getVersion());}
    private static HoldingResponse holding(HoldingEntity e){return new HoldingResponse(e.getId(),e.getOwnerId(),e.getAccountId(),e.getSymbol(),e.getShares(),e.getCostBasisCents(),e.getMarketValueCents(),e.getSource(),e.getSourceFile(),e.getImportedAt(),e.getVersion());}
    private static LiabilityResponse liability(LiabilityEntity e){return new LiabilityResponse(e.getId(),e.getOwnerId(),e.getName(),e.getKind(),e.getBalanceCents(),e.getInterestRate(),e.getMinimumPaymentCents(),e.getAccountId(),e.getSource(),e.getSourceFile(),e.getImportedAt(),e.getVersion());}
    private static ObligationResponse obligation(ObligationEntity e){return new ObligationResponse(e.getId(),e.getOwnerId(),e.getName(),e.getCategory(),e.getAmountCents(),e.getFrequency(),e.getNextDueDate(),e.getVersion());}
    private static GoalResponse goal(GoalEntity e){return new GoalResponse(e.getId(),e.getOwnerId(),e.getName(),e.getTargetCents(),e.getCurrentCents(),e.getDeadline(),e.getVersion());}
    private static RuleResponse rule(RuleEntity e){return new RuleResponse(e.getId(),e.getOwnerId(),e.getPattern(),e.getCategory(),e.getAccountId(),e.getKind(),e.getVersion());}
    private static SnapshotResponse snapshot(SnapshotEntity e){return new SnapshotResponse(e.getId(),e.getOwnerId(),e.getDate(),e.getAssetsCents(),e.getLiabilitiesCents(),e.getNetWorthCents(),e.getAccountId(),e.getSource(),e.getSourceFile(),e.getImportedAt(),e.getVersion());}
}
