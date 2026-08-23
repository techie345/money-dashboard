package com.techie345.moneys.imports.provider;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import com.techie345.moneys.financial.CategorySource;
import com.techie345.moneys.financial.TransactionKind;
import com.techie345.moneys.financial.TransferType;
import org.junit.jupiter.api.Test;

class ProviderContractTest {
    private final UUID owner = UUID.randomUUID();
    private final UUID account = UUID.randomUUID();
    private final ProviderContext context = new ProviderContext(owner, account, List.of(), Instant.parse("2026-08-23T10:00:00Z"));

    @Test
    void csvNormalizesMetadataAndReportsMalformedAndDuplicateRows() {
        String csv = "Date,Description,Amount,Category,Transaction ID\n"
                + "2026-01-02,Coffee,-4.25,Food,abc\n"
                + "bad-date,Missing date,3.00,Other,def\n"
                + "2026-01-02,Coffee,-4.25,Food,abc\n";
        ProviderInput input = ProviderInput.file("bank.csv", "generic", Instant.parse("2026-08-22T09:00:00Z"),
                csv.getBytes(StandardCharsets.UTF_8));

        NormalizedImport normalized = new GenericCsvProvider().normalize(input, context);
        ProviderPreview preview = new GenericCsvProvider().preview(input, context);

        assertEquals("bank.csv", normalized.metadata().filename());
        assertEquals("generic", normalized.metadata().profile());
        assertEquals(Instant.parse("2026-08-22T09:00:00Z"), normalized.metadata().importedAt());
        assertEquals("abc", normalized.candidates().getFirst().sourceId());
        assertEquals(425, normalized.candidates().getFirst().amountCents());
        assertEquals(LocalDate.of(2026, 1, 2), normalized.candidates().getFirst().date());
        assertEquals(1, preview.malformedRows().size());
        assertEquals(1, preview.duplicates().size());
        assertTrue(preview.duplicates().getFirst().reason().contains("source identifier"));
    }

    @Test
    void jsonAndManualUseTheSameCanonicalModel() {
        String json = "[{\"date\":\"2026-01-01\",\"merchant\":\"Rent\",\"description\":\"Rent\","
                + "\"amountCents\":120000,\"category\":\"Housing\",\"sourceId\":\"r1\"}]";
        NormalizedImport fromJson = new JsonBackupProvider().normalize(
                ProviderInput.file("backup.json", "backup", Instant.now(), json.getBytes(StandardCharsets.UTF_8)), context);
        NormalizedImport manual = new ManualEntryProvider().normalize(
                ProviderInput.manual("manual", Instant.now(), List.of(
                        new ManualEntry(LocalDate.of(2026, 1, 1), "Rent", "Rent", 120000, "Housing", "r1"))), context);

        assertEquals(fromJson.candidates().getFirst().date(), manual.candidates().getFirst().date());
        assertEquals(fromJson.candidates().getFirst().amountCents(), manual.candidates().getFirst().amountCents());
        assertEquals(0, fromJson.issues().size());
    }

    @Test
    void registrySelectsManualAndPreviewContainsCategorizationTransferAndReviewData() {
        ProviderRegistry registry = new ProviderRegistry(List.of(new GenericCsvProvider(), new JsonBackupProvider(), new ManualEntryProvider()));
        ProviderInput input = ProviderInput.manual("manual", context.now(), List.of(
                new ManualEntry(LocalDate.of(2026, 1, 1), "Bank transfer", "Bank transfer", 1200, "Transfers", "m1")));

        ProviderPreview preview = registry.get(ProviderType.MANUAL).preview(input, context);

        assertEquals(ProviderType.MANUAL, registry.forType(ProviderType.MANUAL).type());
        assertEquals(1L, preview.categorization().get("Transfers"));
        assertEquals(1, preview.transfers().size());
        assertEquals(1, preview.reviewInfo().size());
    }

    @Test
    void csvPreservesExplicitCategorySourceAndClassifiesNonSpendingKinds() {
        String csv = "Date,Description,Amount,Category,Category Source,Kind,Transfer Type,Transaction ID\n"
                + "2026-01-02,Card payment,-4.25,Food,MANUAL,CREDIT_CARD_PAYMENT,CREDIT_CARD_PAYMENT,p1\n"
                + "2026-01-03,Brokerage buy,-10.00,Investing,IMPORTED,INVESTMENT,,p2\n";

        var result = new GenericCsvProvider().normalize(
                ProviderInput.file("bank.csv", "generic", context.now(), csv.getBytes(StandardCharsets.UTF_8)), context);

        assertEquals(CategorySource.MANUAL, result.candidates().get(0).categorySource());
        assertEquals(TransactionKind.CREDIT_CARD_PAYMENT, result.candidates().get(0).kind());
        assertEquals(TransferType.CREDIT_CARD_PAYMENT, result.candidates().get(0).transferType());
        assertEquals(TransactionKind.INVESTMENT, result.candidates().get(1).kind());
    }

    @Test
    void jsonClassifiesPaymentAndInvestmentUsingExplicitAndFallbackFields() {
        String json = "["
                + "{\"date\":\"2026-01-01\",\"merchant\":\"Payment\",\"description\":\"Payment\",\"amountCents\":1200,\"kind\":\"CREDIT_CARD_PAYMENT\"},"
                + "{\"date\":\"2026-01-02\",\"merchant\":\"Broker\",\"description\":\"Buy shares\",\"amountCents\":1200,\"symbol\":\"ABC\"}"
                + "]";

        var result = new JsonBackupProvider().normalize(
                ProviderInput.file("backup.json", "backup", context.now(), json.getBytes(StandardCharsets.UTF_8)), context);

        assertEquals(TransactionKind.CREDIT_CARD_PAYMENT, result.candidates().get(0).kind());
        assertEquals(TransferType.CREDIT_CARD_PAYMENT, result.candidates().get(0).transferType());
        assertEquals(TransactionKind.INVESTMENT, result.candidates().get(1).kind());
    }

    @Test
    void duplicateIdentityIncludesAccountAndKind() {
        var first = new NormalizedCandidate(1, account, LocalDate.of(2026, 1, 1), "Store", "Store", 100,
                TransactionKind.SPENDING, "Food", CategorySource.IMPORTED, null);
        var otherAccount = new NormalizedCandidate(2, UUID.randomUUID(), first.date(), first.merchant(), "Store", 100,
                TransactionKind.SPENDING, "Food", CategorySource.IMPORTED, null);
        var otherKind = new NormalizedCandidate(3, account, first.date(), first.merchant(), "Store", 100,
                TransactionKind.INCOME, "Food", CategorySource.IMPORTED, null);

        assertTrue(GenericCsvProvider.duplicates(List.of(first, new NormalizedCandidate(4, account, first.date(),
                first.merchant(), "Store", 100, TransactionKind.SPENDING, "Food", CategorySource.IMPORTED, null))).size() == 1);
        assertTrue(GenericCsvProvider.duplicates(List.of(first, otherAccount, otherKind)).isEmpty());
        var withSource = new NormalizedCandidate(5, account, first.date(), first.merchant(), "Store", 100,
                TransactionKind.SPENDING, "Food", CategorySource.IMPORTED, "same-source");
        var sourceOtherAccount = new NormalizedCandidate(6, otherAccount.accountId(), first.date(), first.merchant(), "Store", 100,
                TransactionKind.INCOME, "Food", CategorySource.IMPORTED, "same-source");
        assertTrue(GenericCsvProvider.duplicates(List.of(withSource, sourceOtherAccount)).isEmpty());
    }

    @Test
    void csvReportsQuotedMultilineAndWrongColumnRowsWithoutDroppingThem() {
        String csv = "Date,Description,Amount\n"
                + "2026-01-01,\"Store, Main\",1.00\n"
                + "2026-01-02,\"Two\nlines\",2.00\n"
                + "2026-01-03,Too,many,columns\n";

        var result = new GenericCsvProvider().normalize(
                ProviderInput.file("rows.csv", "generic", context.now(), csv.getBytes(StandardCharsets.UTF_8)), context);

        assertEquals(2, result.candidates().size());
        assertEquals("Store, Main", result.candidates().get(0).merchant());
        assertEquals(1, result.issues().size());
        assertEquals(5, result.issues().get(0).rowNumber());
    }

    @Test
    void jsonRejectsFractionalAndOutOfRangeMoneyWithoutTruncation() {
        String json = "[{\"date\":\"2026-01-01\",\"merchant\":\"fraction\",\"amountCents\":1.5},"
                + "{\"date\":\"2026-01-01\",\"merchant\":\"overflow\",\"amountCents\":9223372036854775808}]";

        var result = new JsonBackupProvider().normalize(
                ProviderInput.file("backup.json", "backup", context.now(), json.getBytes(StandardCharsets.UTF_8)), context);

        assertTrue(result.candidates().isEmpty());
        assertEquals(2, result.issues().size());
    }

    @Test
    void malformedIssueValuesAreBounded() {
        String value = "Date,Description,Amount\n2026-01-01," + "x".repeat(1000) + ",bad";
        var result = new GenericCsvProvider().normalize(
                ProviderInput.file("rows.csv", "generic", context.now(), value.getBytes(StandardCharsets.UTF_8)), context);

        assertTrue(result.issues().getFirst().value().length() <= 256);
    }
}
