package com.techie345.moneys.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParts;
import static org.springframework.restdocs.request.RequestDocumentation.partWithName;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.techie345.moneys.financial.AccountKind;
import com.techie345.moneys.audit.ChangeOperation;
import com.techie345.moneys.financial.CalculationReadModels;
import com.techie345.moneys.financial.FinancialCalculationService;
import com.techie345.moneys.financial.FinancialMutationService;
import com.techie345.moneys.financial.api.FinancialApiExceptionHandler;
import com.techie345.moneys.financial.api.FinancialResourceController;
import com.techie345.moneys.financial.api.FinancialResourceDtos;
import com.techie345.moneys.financial.api.VersionConflictException;
import com.techie345.moneys.financial.account.AccountEntity;
import com.techie345.moneys.financial.account.AccountRepository;
import com.techie345.moneys.financial.asset.AssetRepository;
import com.techie345.moneys.financial.goal.GoalRepository;
import com.techie345.moneys.financial.holding.HoldingRepository;
import com.techie345.moneys.financial.liability.LiabilityRepository;
import com.techie345.moneys.financial.obligation.ObligationRepository;
import com.techie345.moneys.financial.rule.RuleRepository;
import com.techie345.moneys.financial.snapshot.SnapshotRepository;
import com.techie345.moneys.financial.transaction.TransactionRepository;
import com.techie345.moneys.identity.CurrentUserArgumentResolver;
import com.techie345.moneys.identity.ProfileController;
import com.techie345.moneys.identity.UserRepository;
import com.techie345.moneys.imports.ImportController;
import com.techie345.moneys.imports.ImportResponse;
import com.techie345.moneys.imports.ImportService;
import com.techie345.moneys.sync.SyncController;
import com.techie345.moneys.sync.SyncResponse;
import com.techie345.moneys.sync.SyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;

@WebMvcTest({ProfileController.class, FinancialResourceController.class, ImportController.class, SyncController.class})
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(RestDocumentationExtension.class)
@TestPropertySource(properties = {"app.database.enabled=false", "app.imports.enabled=true", "app.financial.api.enabled=true"})
@Import({CurrentUserArgumentResolver.class, FinancialApiExceptionHandler.class, ApiDocumentationTest.ResolverConfiguration.class})
class ApiDocumentationTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ACCOUNT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Autowired WebApplicationContext context;
    private MockMvc mockMvc;
    @MockitoBean UserRepository users;
    @MockitoBean AccountRepository accounts;
    @MockitoBean TransactionRepository transactions;
    @MockitoBean AssetRepository assets;
    @MockitoBean HoldingRepository holdings;
    @MockitoBean LiabilityRepository liabilities;
    @MockitoBean ObligationRepository obligations;
    @MockitoBean GoalRepository goals;
    @MockitoBean RuleRepository rules;
    @MockitoBean SnapshotRepository snapshots;
    @MockitoBean FinancialMutationService mutations;
    @MockitoBean FinancialCalculationService calculations;
    @MockitoBean ImportService imports;
    @MockitoBean SyncService sync;
    @MockitoBean com.techie345.moneys.audit.AuditService audit;

    @BeforeEach
    void configureDocumentation(RestDocumentationContextProvider restDocumentation) {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(documentationConfiguration(restDocumentation)
                        .operationPreprocessors().withRequestDefaults(prettyPrint()).withResponseDefaults(prettyPrint()))
                .build();
    }

    @Test
    void documentsAuthenticationStatus() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andDo(document("authentication-status", responseFields(
                        fieldWithPath("status").description("HTTP status."),
                        fieldWithPath("code").description("Stable error code."),
                        fieldWithPath("message").description("Human-readable error."),
                        subsectionWithPath("details").description("Additional error details."),
                        fieldWithPath("traceId").description("Request trace identifier."),
                        fieldWithPath("timestamp").description("Error timestamp."))));
    }

    @Test
    void documentsAccountCrudAndConflict() throws Exception {
        AccountEntity account = syntheticAccount();
        when(mutations.create(eq(accounts), any(AccountEntity.class), any())).thenReturn(account);
        when(accounts.findByIdAndOwnerId(ACCOUNT_ID, OWNER)).thenReturn(Optional.of(account));
        when(mutations.update(any(), eq(0L), any(), eq(accounts), any(), any())).thenReturn(account);

        mockMvc.perform(post("/api/v1/accounts").principal(() -> OWNER.toString()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"institution\":\"Example Bank\",\"name\":\"Checking\",\"kind\":\"cash\",\"balanceCents\":12500}"))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/v1/accounts/" + ACCOUNT_ID)).andDo(document("account-create", requestFields(
                        fieldWithPath("institution").description("Institution name."), fieldWithPath("name").description("Account name."),
                        fieldWithPath("kind").description("Account kind."), fieldWithPath("balanceCents").description("Balance in integer cents."),
                        fieldWithPath("lastImportedAt").type(STRING).optional().description("Last import timestamp."), fieldWithPath("source").type(STRING).optional().description("Data source."),
                        fieldWithPath("sourceFile").type(STRING).optional().description("Source filename."), fieldWithPath("importedAt").type(STRING).optional().description("Import timestamp.")), accountFields(),
                        responseHeaders(headerWithName("Location").description("URI of the created account."))));

        mockMvc.perform(get("/api/v1/accounts/{id}", ACCOUNT_ID).principal(() -> OWNER.toString()))
                .andExpect(status().isOk()).andDo(document("account-read", responseFields(accountFieldDescriptors())));
        mockMvc.perform(patch("/api/v1/accounts/{id}", ACCOUNT_ID).principal(() -> OWNER.toString()).header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"institution\":\"Example Bank\",\"name\":\"Checking\",\"kind\":\"cash\",\"balanceCents\":13000}"))
                .andExpect(status().isOk()).andExpect(header().string("ETag", "\"0\""))
                .andDo(document("account-update", requestHeaders(headerWithName("If-Match").description("Expected account version.")), accountRequestFields(),
                        responseHeaders(headerWithName("ETag").description("Current account version.")), responseFields(accountFieldDescriptors())));
        when(mutations.delete(any(), eq(0L), any(), eq(accounts), any())).thenReturn(true);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/accounts/{id}", ACCOUNT_ID)
                        .principal(() -> OWNER.toString()).header("If-Match", "\"0\""))
                .andExpect(status().isNoContent()).andDo(document("account-delete",
                        requestHeaders(headerWithName("If-Match").description("Expected account version."))));
    }

    @Test
    void documentsTransactionValidation() throws Exception {
                mockMvc.perform(post("/api/v1/transactions").principal(() -> OWNER.toString()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-01-01\",\"merchant\":\"\",\"description\":\"\",\"amountCents\":0,\"kind\":\"spending\",\"category\":\"food\",\"accountId\":\"00000000-0000-0000-0000-000000000002\",\"source\":\"manual\",\"sourceFile\":\"manual\"}"))
                .andExpect(status().isBadRequest()).andDo(document("transaction-validation", transactionRequestFields(), responseFields(
                        fieldWithPath("status").description("HTTP status."), fieldWithPath("code").description("Stable error code."),
                        fieldWithPath("message").description("Validation summary."), fieldWithPath("details[].description").description("Description validation message."),
                        fieldWithPath("details[].amountCents").description("Amount validation message."), fieldWithPath("details[].merchant").description("Merchant validation message."),
                        fieldWithPath("traceId").description("Request trace identifier."), fieldWithPath("timestamp").description("Error timestamp."))));
    }

    @Test
    void documentsDashboardOverview() throws Exception {
        when(calculations.overview(OWNER)).thenReturn(new CalculationReadModels.Overview(10000, 4000, 25000, .6));
        mockMvc.perform(get("/api/v1/dashboard/overview").principal(() -> OWNER.toString())).andExpect(status().isOk())
                .andDo(document("dashboard-overview", responseFields(fieldWithPath("incomeCents").description("Income in cents."),
                        fieldWithPath("spendingCents").description("Spending in cents."), fieldWithPath("netWorthCents").description("Net worth in cents."),
                        fieldWithPath("savingsRate").description("Savings rate."))));
    }

    @Test
    void documentsImportPreviewAndCommit() throws Exception {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000003");
        when(imports.create(any(), eq(OWNER))).thenReturn(new ImportResponse(id, "confirmation-token", 0, "PREVIEW"));
        when(imports.preview(id, OWNER)).thenReturn(new ImportResponse(id, "confirmation-token", 0, "PREVIEW"));
        when(imports.commit(id, OWNER, "confirmation-token", 0)).thenReturn(new ImportResponse(id, "confirmation-token", 1, "COMMITTED"));
        mockMvc.perform(multipart("/api/v1/imports?profile=generic").file("file", "Date,Description,Amount\n2026-01-01,Sample,-2.00".getBytes())
                        .principal(() -> OWNER.toString())).andExpect(status().isCreated())
                .andDo(document("import-create", queryParameters(parameterWithName("profile").description("Import provider profile.")),
                        requestParts(partWithName("file").description("CSV file to import.")), responseFields(importResponseFieldDescriptors())));
        mockMvc.perform(get("/api/v1/imports/{id}/preview", id).principal(() -> OWNER.toString())).andExpect(status().isOk())
                .andDo(document("import-preview", responseFields(importResponseFieldDescriptors())));
        mockMvc.perform(post("/api/v1/imports/{id}/commit", id).principal(() -> OWNER.toString()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmationToken\":\"confirmation-token\",\"version\":0}"))
                .andExpect(status().isOk()).andDo(document("import-commit", requestFields(fieldWithPath("confirmationToken").description("Preview confirmation token."), fieldWithPath("version").description("Expected version.")), responseFields(importResponseFieldDescriptors())));
    }

    @Test
    void documentsVersionConflict() throws Exception {
        when(accounts.findByIdAndOwnerId(ACCOUNT_ID, OWNER)).thenReturn(Optional.of(syntheticAccount()));
        when(mutations.update(any(), eq(0L), any(), eq(accounts), any(), any())).thenThrow(new VersionConflictException());
        mockMvc.perform(patch("/api/v1/accounts/{id}", ACCOUNT_ID).principal(() -> OWNER.toString()).header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"institution\":\"Example Bank\",\"name\":\"Checking\",\"kind\":\"cash\",\"balanceCents\":13000}"))
                .andExpect(status().isConflict()).andDo(document("version-conflict", requestHeaders(headerWithName("If-Match").description("Expected account version.")), accountRequestFields(), responseFields(
                        fieldWithPath("status").description("HTTP status."), fieldWithPath("code").description("Stable error code."), fieldWithPath("message").description("Conflict summary."),
                        fieldWithPath("details[]").description("Current resource details."), fieldWithPath("traceId").description("Request trace identifier."), fieldWithPath("timestamp").description("Error timestamp."))));
    }

    @Test
    void documentsSyncCursorResponse() throws Exception {
        when(sync.read(OWNER, 8, 10)).thenReturn(new SyncResponse(List.of(new SyncResponse.Change(9, ChangeOperation.CREATED, "account", ACCOUNT_ID, "record-1", null, Instant.parse("2026-01-01T00:00:00Z"), 0, "{}")), 9));
        mockMvc.perform(get("/api/v1/sync").param("cursor", "8").param("limit", "10").principal(() -> OWNER.toString()))
                .andExpect(status().isOk()).andDo(document("sync-cursor", queryParameters(parameterWithName("cursor").description("Exclusive change sequence cursor."), parameterWithName("limit").description("Maximum changes to return.")), responseFields(
                        fieldWithPath("changes[].sequence").description("Monotonic change sequence."), fieldWithPath("changes[].operation").description("Change operation."), fieldWithPath("changes[].resourceType").description("Resource type."),
                        fieldWithPath("changes[].resourceId").description("Resource identifier."), fieldWithPath("changes[].sourceId").description("Source identifier."), fieldWithPath("changes[].importId").optional().description("Import identifier."),
                        fieldWithPath("changes[].changedAt").description("Change timestamp."), fieldWithPath("changes[].version").description("Resource version."), fieldWithPath("changes[].payload").description("Safe resource snapshot."),
                        fieldWithPath("nextCursor").description("Cursor for the next page."))));
    }

    private static org.springframework.restdocs.payload.FieldDescriptor[] accountFieldDescriptors() {
        return new org.springframework.restdocs.payload.FieldDescriptor[] {
                fieldWithPath("id").description("Account identifier."), fieldWithPath("ownerId").description("Owning user identifier."), fieldWithPath("institution").description("Institution name."),
                fieldWithPath("name").description("Account name."), fieldWithPath("kind").description("Account kind."), fieldWithPath("balanceCents").description("Balance in integer cents."),
                fieldWithPath("lastImportedAt").type(STRING).optional().description("Last import timestamp."), fieldWithPath("source").type(STRING).optional().description("Data source."), fieldWithPath("sourceFile").type(STRING).optional().description("Source filename."),
                fieldWithPath("importedAt").type(STRING).optional().description("Import timestamp."), fieldWithPath("version").description("Optimistic locking version.") };
    }

    private static org.springframework.restdocs.payload.ResponseFieldsSnippet accountFields() {
        return responseFields(accountFieldDescriptors());
    }

    private static org.springframework.restdocs.payload.RequestFieldsSnippet accountRequestFields() {
        return requestFields(fieldWithPath("institution").description("Institution name."), fieldWithPath("name").description("Account name."),
                fieldWithPath("kind").description("Account kind."), fieldWithPath("balanceCents").description("Balance in integer cents."),
                fieldWithPath("lastImportedAt").type(STRING).optional().description("Last import timestamp."), fieldWithPath("source").type(STRING).optional().description("Data source."),
                fieldWithPath("sourceFile").type(STRING).optional().description("Source filename."), fieldWithPath("importedAt").type(STRING).optional().description("Import timestamp."));
    }

    private static org.springframework.restdocs.payload.RequestFieldsSnippet transactionRequestFields() {
        return requestFields(fieldWithPath("date").description("Transaction date."), fieldWithPath("merchant").description("Merchant name."),
                fieldWithPath("description").description("Transaction description."), fieldWithPath("amountCents").description("Amount in integer cents."),
                fieldWithPath("kind").description("Transaction kind."), fieldWithPath("category").description("Transaction category."),
                fieldWithPath("accountId").description("Owning account identifier."), fieldWithPath("source").description("Data source."),
                fieldWithPath("sourceFile").description("Source filename."));
    }

    private static org.springframework.restdocs.payload.FieldDescriptor[] importResponseFieldDescriptors() {
        return new org.springframework.restdocs.payload.FieldDescriptor[] {
                fieldWithPath("id").type(STRING).description("Import identifier."),
                fieldWithPath("confirmationToken").type(STRING).description("Confirmation token."),
                fieldWithPath("version").type(NUMBER).description("Import version."),
                fieldWithPath("status").type(STRING).description("Import status."),
                fieldWithPath("preview").type(OBJECT).optional().description("Validated preview details.") };
    }

    private static AccountEntity syntheticAccount() {
        AccountEntity account = new AccountEntity(OWNER, "Example Bank", "Checking", AccountKind.CASH, 12500);
        ReflectionTestUtils.setField(account, "id", ACCOUNT_ID);
        return account;
    }

    @TestConfiguration
    static class ResolverConfiguration implements WebMvcConfigurer {
        @Bean CurrentUserArgumentResolver currentUserArgumentResolver() { return new CurrentUserArgumentResolver(); }
        @Bean SyncController syncController(SyncService service) { return new SyncController(service); }
    }
}
