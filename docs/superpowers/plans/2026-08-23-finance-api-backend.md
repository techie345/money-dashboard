# Finance API Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split Interactive Moneys into a Spring Boot/PostgreSQL backend and a dumb React frontend with Google OIDC, complete financial CRUD, server-owned imports, calculations, and multi-device synchronization.

**Architecture:** Build a modular monolith in a new `backend/` application. Keep identity, financial resources, imports, calculations, sync, and audit behind package boundaries, with PostgreSQL as the canonical store and REST DTOs as the frontend contract. Convert the existing frontend from local dataset ownership to an authenticated API client while retaining only a cache and UI state in IndexedDB.

**Tech Stack:** Java 21, Spring Boot 4.1.1, Spring Framework 7.0.x, Spring Security 7.1.1, Spring Modulith 2.1.0, Spring Session JDBC 4.1.1, Spring MVC, Spring Data JPA, PostgreSQL, Flyway, Actuator, Spring REST Docs 3.0.6, Testcontainers, Gradle 9.x, React 19, TypeScript, Vite, Vitest.

**Version research:** As of 2026-08-23, official Spring pages list Spring Boot 4.1.1, Spring Security 7.1.0 on its project page and 7.1.1 in the current reference documentation, Spring Modulith 2.1.0, and Spring Session 4.1.1. Use the latest stable patch available from Spring Initializr/BOM at scaffold time if a newer patch exists, never a milestone or snapshot. Boot 4.1.1 requires Java 17+ and supports Gradle 8.14+ or 9.x; Java 21 is selected for LTS stability.

**Best-practice decisions:** Use Spring Modulith to verify package boundaries and generate module documentation. Use Spring Session JDBC so sessions work across multiple backend instances without in-memory affinity. Use Flyway as the only schema migration authority, `@ConfigurationProperties` for typed external configuration, Actuator with narrowly exposed management endpoints, and Spring REST Docs generated from MockMvc contract tests so API documentation cannot drift silently.

---

## Session Progress (updated 2026-08-23)

Executed on branch `add_api` via subagent-driven development. Tasks 1 through 6 are implemented and committed in `38fa0af` (`feat: secure backend and add audit sync`). The push is pending because this environment has no GitHub credentials.

| Task | Status |
|------|--------|
| 1 — Scaffold | Done and committed |
| 1a — Modulith/config/actuator | Done and committed |
| 2 — PostgreSQL schema/users | Done, Testcontainers tests pass, committed |
| 3 — Financial domain + CRUD | Done incl. snapshots CRUD, ETags/If-Match, error contract, dashboard calc endpoints, committed |
| 4 — Import pipeline | Done incl. preview non-mutation, rules, duplicates w/ commit-time races, staging expiry, idempotent transactional commit, committed |
| 5 — Google OIDC security | Done incl. CSRF, CORS restrictions, CurrentUser integration, JDBC sessions, `/me` and `/profile`, committed |
| 6 — Audit events + cursor sync | Done incl. transactional audit snapshots, owner-scoped sequences, tombstones, bounded cursor sync, committed |
| 6a — REST Docs | Not started |
| 7–9 — Frontend conversion, import UI, deployment | Out of scope this session, untouched |

### Current handoff and loose ends

- **Verification** — `JAVA_HOME=/usr/lib/jvm/java-21-openjdk PATH=/usr/lib/jvm/java-21-openjdk/bin:$PATH gradle clean test` passes with 72 tests and 0 failures; `gradle build` also passes.
- **Push** — commit `38fa0af` is local on `add_api`; `git push -u origin add_api` is blocked by missing GitHub credentials.
- **Gradle wrapper absent** — all documented `./gradlew` commands fail until `gradle wrapper` is generated and committed.
- **Task 3 residual edge case** — a database-level optimistic-lock race cannot currently include the conflicting representation in its `409` response because Spring's exception does not retain that entity; explicit `If-Match` conflicts do include the current DTO.
- **Task 4 residual gaps** — transactional rollback/uniqueness-race integration tests against real PostgreSQL are thin (service tests mock repositories); CSV provider set is generic-only (institution-specific providers deferred); preview issue values are truncated but not semantically redacted.
- **README not updated** — still describes browser-local-only architecture; update together with Tasks 7–9 or before merge.
- **CI** — `.github/workflows/backend-build.yml` (Task 9) does not exist; frontend CI does not run backend checks.

### Environment notes for resuming

- Java 21 installed at `/usr/lib/jvm/java-21-openjdk` but system default is Java 26. Prefix Gradle commands with `JAVA_HOME=/usr/lib/jvm/java-21-openjdk PATH=/usr/lib/jvm/java-21-openjdk/bin:$PATH`.
- System Gradle 9.7.0 is used directly (`gradle`, no `./gradlew` wrapper generated yet).
- Rootless Podman provides the Docker API for Testcontainers: `podman.socket` user service is enabled, and `~/.testcontainers.properties` pins `docker.host=unix:///run/user/1000/podman/podman.sock` with `ryuk.disabled=true`.
- Last full verification passed: `gradle clean test` (42+ tests, 0 failures) under Java 21 + Podman.
- Boot 4.1.1 required adding `spring-boot-starter-flyway`; migrations live in `backend/src/main/resources/db/migration/`.

### Suggested resume order

1. Push commit `38fa0af` after configuring GitHub credentials.
2. Implement Task 6a (REST Docs) if API contract documentation is required next.
3. Implement Tasks 7–9 for frontend API conversion, server-owned import UI, deployment, CI, and end-to-end verification.

---

## File Map

Create a new independent backend application under `backend/`:

- `backend/settings.gradle`: Gradle project name.
- `backend/build.gradle`: Spring Boot 4.1.1 BOM, database, security, session, migration, observability, documentation, test, and quality dependencies.
- `backend/src/main/java/com/techie345/moneys/MoneyApplication.java`: Spring Boot entry point.
- `backend/src/main/java/com/techie345/moneys/{identity,financial,imports,dashboard,audit,sync}/`: module-owned controllers, DTOs, services, domain types, repositories, and mappers.
- `backend/src/main/resources/application.yml`: safe defaults and environment variable bindings.
- `backend/src/main/resources/db/migration/V1__initial_schema.sql`: initial relational schema.
- `backend/src/test/...`: unit, MVC, repository, security, and workflow tests.
- `backend/Dockerfile`: reproducible backend image.
- `docker-compose.yml`: local PostgreSQL and backend services.

Modify the frontend in focused areas:

- `src/api/`: typed HTTP client, API DTOs, error parsing, and authentication helpers.
- `src/App.tsx`: replace bootstrap/local persistence with authenticated server loading and mutation callbacks.
- `src/data/storage.ts`: reduce IndexedDB to cache management after the API cache is established.
- `src/components/pages/*.tsx` and `src/components/dashboard/*.tsx`: consume API view models and expose loading/error/empty states.
- `src/import/`: replace local commit ownership with import API preview and commit calls.
- `src/domain/`: retain display types only where useful; remove duplicate calculations after backend responses are adopted.
- `src/api/*.test.ts`, component tests, and end-to-end tests: verify API states and user workflows.
- `README.md`: document two applications, local Compose startup, OAuth configuration, and source boundaries.

## Task 1: Scaffold the Spring Boot Application

**Files:**
- Create: `backend/settings.gradle`
- Create: `backend/build.gradle`
- Create: `backend/src/main/java/com/techie345/moneys/MoneyApplication.java`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/test/java/com/techie345/moneys/MoneyApplicationTest.java`
- Create: `backend/Dockerfile`

- [ ] **Step 1: Write the failing application smoke test**

Create a Spring context test that asserts the application starts with an isolated test profile:

```java
@SpringBootTest
class MoneyApplicationTest {
    @Test
    void starts() {
    }
}
```

- [ ] **Step 2: Run the test to verify the scaffold is absent**

Run: `cd backend && ./gradlew test --tests '*MoneyApplicationTest'`

Expected: FAIL because the Gradle wrapper/application files do not exist yet.

- [ ] **Step 3: Add the minimal Spring Boot 4 project**

Generate the project from Spring Initializr using Spring Boot `4.1.1`, Java `21`, and Gradle `9.x`. Use the Boot-managed dependency versions rather than specifying versions for Spring modules. Include the Boot 4 MVC web starter, validation, JPA, PostgreSQL, Flyway, Security, OAuth2 Client, JDBC Session, Actuator, Spring Modulith, and test dependencies. Add Spring REST Docs as a test dependency with the Gradle Asciidoctor plugin. The application entry point must be:

```java
package com.techie345.moneys;

@SpringBootApplication
public class MoneyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MoneyApplication.class, args);
    }
}
```

Configure `server.port`, datasource, Flyway, JDBC session storage, session timeout, Actuator exposure, and frontend CORS settings through environment-backed properties and typed `@ConfigurationProperties` records. Do not put client secrets in the repository. The Dockerfile must run the built jar as a non-root user. Keep Actuator separate from the public API port or expose only `/actuator/health`, `/actuator/info`, and `/actuator/metrics` through an authenticated or allow-listed management interface.

- [ ] **Step 4: Run the smoke test to verify it passes**

Run: `cd backend && ./gradlew test --tests '*MoneyApplicationTest'`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend
git commit -m "feat: scaffold spring backend"
```

## Task 1a: Establish Modular Boundaries and Production Conventions

**Files:**
- Modify: `backend/build.gradle`
- Create: `backend/src/test/java/com/techie345/moneys/ArchitectureTest.java`
- Create: `backend/src/main/java/com/techie345/moneys/shared/` classes
- Create: `backend/src/main/java/com/techie345/moneys/configuration/` classes
- Create: `backend/src/test/java/com/techie345/moneys/ActuatorTest.java`

- [ ] **Step 1: Add Spring Modulith verification**

Declare each business module as a direct subpackage of `com.techie345.moneys`: `identity`, `financial`, `imports`, `dashboard`, `audit`, and `sync`. Add an `ApplicationModules.of(MoneyApplication.class).verify()` test. The test must reject cycles and access to another module's internal package.

- [ ] **Step 2: Add typed configuration and observability tests**

Create immutable `@ConfigurationProperties` for database/session/CORS limits. Test that health is public, financial data is never returned by health/info endpoints, and metrics are not exposed on arbitrary routes. Use structured logs with request correlation IDs and never log raw transaction descriptions, source files, OAuth tokens, or database credentials.

- [ ] **Step 3: Run architecture and actuator tests**

Run: `cd backend && ./gradlew test --tests '*ArchitectureTest' --tests '*ActuatorTest'`

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add backend
git commit -m "feat: enforce spring module boundaries"
```

## Task 2: Add PostgreSQL Schema and User Ownership

**Files:**
- Create: `backend/src/main/resources/db/migration/V1__initial_schema.sql`
- Create: `backend/src/main/java/com/techie345/moneys/identity/UserEntity.java`
- Create: `backend/src/main/java/com/techie345/moneys/identity/UserRepository.java`
- Create: `backend/src/main/java/com/techie345/moneys/identity/UserAccount.java`
- Create: `backend/src/test/java/com/techie345/moneys/identity/UserRepositoryTest.java`
- Create: `backend/src/test/resources/application-test.yml`

- [ ] **Step 1: Write repository tests against PostgreSQL**

Test that a user can be saved and loaded by Google subject, and that two users cannot read one another's records. Use Testcontainers PostgreSQL for repository tests rather than H2 so UUID, timestamp, indexes, and transaction behavior match production.

- [ ] **Step 2: Run the repository test to verify it fails**

Run: `cd backend && ./gradlew test --tests '*UserRepositoryTest'`

Expected: FAIL because the schema and repository do not exist.

- [ ] **Step 3: Create the initial schema and entities**

Create `app_user` with UUID primary key, unique Google subject, display name, email, created timestamp, and updated timestamp. Add ownership columns and indexes to every future financial table in the migration contract. Use `BIGINT` for all `*_cents` values and `NUMERIC` only for share quantities/rates where integer cents do not apply.

The user entity must expose no password or provider token fields. Repositories must require an owner ID in lookup methods instead of exposing unscoped `findAll()` methods.

- [ ] **Step 4: Run the repository test to verify it passes**

Run: `cd backend && ./gradlew test --tests '*UserRepositoryTest'`

Expected: PASS against PostgreSQL.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/resources/db backend/src/main/java/com/techie345/moneys/identity backend/src/test
git commit -m "feat: add postgres user ownership schema"
```

## Task 3: Port the Financial Domain and CRUD Resources

**Files:**
- Modify: `backend/src/main/resources/db/migration/V1__initial_schema.sql`
- Create: `backend/src/main/java/com/techie345/moneys/financial/{account,transaction,asset,holding,liability,obligation,goal,rule,snapshot}/...`
- Create: `backend/src/main/java/com/techie345/moneys/financial/api/FinancialResourceController.java`
- Create: `backend/src/main/java/com/techie345/moneys/financial/api/FinancialResourceDtos.java`
- Create: `backend/src/test/java/com/techie345/moneys/financial/...`

- [ ] **Step 1: Write domain tests for money and record invariants**

Port focused tests from `src/domain/calculations.test.ts`, `src/domain/rules.test.ts`, and the validation behavior in `src/data/seed.ts`. Cover positive amount magnitudes, integer cents, valid transaction kinds/categories, account ownership, manual category preservation, and exclusion of transfer/credit-card-payment records from spending totals.

- [ ] **Step 2: Run the focused tests to verify they fail**

Run: `cd backend && ./gradlew test --tests '*Financial*'`

Expected: FAIL because the Java domain types and services do not exist.

- [ ] **Step 3: Implement domain types and schema tables**

Create Java records/enums for the fields in `src/domain/types.ts`. Preserve source, source file, imported timestamp, source ID, review status, category source, transfer type, and optional investment fields. Use immutable command objects and explicit transaction direction. Add foreign keys from records to `app_user` and account IDs where applicable.

Implement pure calculation services for overview, spending, income, investment, and net worth. The service must calculate with `long` cents and return typed read models. Create repository interfaces whose methods all accept the authenticated user ID.

- [ ] **Step 4: Add CRUD controller tests**

Test `GET`, `POST`, `PATCH`, and `DELETE` for each resource group under `/api/v1`, including validation failures, `404` for another user's ID, and preservation of manually assigned categories. Assert response DTOs never expose persistence implementation details.

- [ ] **Step 5: Implement CRUD endpoints**

Add resource-specific application services and controllers. Use request DTO validation, transaction boundaries on mutations, `201` for creates, `204` for deletes, and explicit `404` responses. Add `If-Match`/version handling to mutable records so stale writes return `409` rather than overwrite newer data.

- [ ] **Step 6: Run backend domain and API tests**

Run: `cd backend && ./gradlew test --tests '*financial*'`

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add backend
git commit -m "feat: add financial resources and calculations"
```

## Task 4: Implement Provider and Import Workflows

**Files:**
- Create: `backend/src/main/java/com/techie345/moneys/imports/provider/FinancialDataProvider.java`
- Create: `backend/src/main/java/com/techie345/moneys/imports/provider/AbstractFileProvider.java`
- Create: `backend/src/main/java/com/techie345/moneys/imports/provider/GenericCsvProvider.java`
- Create: `backend/src/main/java/com/techie345/moneys/imports/provider/JsonBackupProvider.java`
- Create: `backend/src/main/java/com/techie345/moneys/imports/provider/ManualEntryProvider.java`
- Create: `backend/src/main/java/com/techie345/moneys/imports/{ImportEntity,ImportRepository,ImportService,ImportController}.java`
- Create: `backend/src/test/java/com/techie345/moneys/imports/...`
- Modify: `backend/src/main/resources/db/migration/V1__initial_schema.sql`

- [ ] **Step 1: Write provider contract tests**

Test that a CSV provider produces normalized candidates without writing database records, preserves source filename/profile/import timestamp/source identifiers, reports malformed rows, and does not silently remove duplicates. Test JSON backup and manual entry through the same canonical normalized import model.

- [ ] **Step 2: Run provider tests to verify they fail**

Run: `cd backend && ./gradlew test --tests '*Provider*' --tests '*Import*'`

Expected: FAIL because provider and import types do not exist.

- [ ] **Step 3: Implement the provider port and adapters**

Define the provider contract as:

```java
public interface FinancialDataProvider {
    ProviderType type();
    ProviderMetadata metadata();
    ProviderPreview preview(ProviderInput input, ProviderContext context);
    NormalizedImport normalize(ProviderInput input, ProviderContext context);
}
```

Keep `AbstractFileProvider` limited to common header/text normalization. Implement CSV parsing and JSON backup parsing without persistence. Add a registry that selects providers by source type/profile.

- [ ] **Step 4: Implement preview, validation, deduplication, and categorization**

Create an uncommitted import staging record containing source metadata and normalized candidates. Run validation, duplicate analysis, transfer/investment classification, merchant rules, and review-status assignment against staged data. Preserve existing manual categories when rules run. Return all invalid rows and duplicate candidates with reasons.

- [ ] **Step 5: Add import controller tests**

Test `POST /api/v1/imports`, `GET /api/v1/imports/{id}/preview`, `POST /api/v1/imports/{id}/commit`, and `DELETE /api/v1/imports/{id}`. Assert preview leaves canonical tables unchanged, commit is explicit and transactional, repeated commit is idempotent, and another user cannot access the staged import.

- [ ] **Step 6: Implement import endpoints and commit transaction**

Accept multipart uploads or structured manual input, persist only staging data on creation, and make commit require a confirmation token/version from the latest preview. Commit all related records in one transaction and append an audit record. Reject expired or already-discarded previews with a stable error code.

- [ ] **Step 7: Run import tests**

Run: `cd backend && ./gradlew test --tests '*Import*' --tests '*Provider*'`

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add backend
git commit -m "feat: add server-side import pipeline"
```

## Task 5: Add Google OIDC and API Security

**Files:**
- Create: `backend/src/main/java/com/techie345/moneys/identity/SecurityConfig.java`
- Create: `backend/src/main/java/com/techie345/moneys/identity/OidcUserService.java`
- Create: `backend/src/main/java/com/techie345/moneys/identity/CurrentUser.java`
- Create: `backend/src/test/java/com/techie345/moneys/identity/SecurityConfigTest.java`
- Modify: `backend/src/main/resources/application.yml`

- [ ] **Step 1: Write security tests**

Test that health and OAuth endpoints are public, financial endpoints reject anonymous requests, authenticated requests resolve users by Google subject, CSRF protects browser mutations, and configured frontend origins pass CORS while unknown origins fail.

- [ ] **Step 2: Run security tests to verify they fail**

Run: `cd backend && ./gradlew test --tests '*Security*'`

Expected: FAIL because security configuration is absent.

- [ ] **Step 3: Configure backend-managed OIDC**

Configure Spring Security 7.1 OAuth2 client login at `/oauth2/authorization/google` and callback handling using the authorization-code flow. Persist or update the local user from the verified OIDC subject. Use secure HttpOnly SameSite cookies, enable CSRF for cookie-authenticated mutations, and keep Google tokens server-side. Do not add passwords or credential storage.

- [ ] **Step 4: Add authenticated-user context**

Expose a request-scoped/current-user abstraction that application services require. Ensure every resource and import query uses that user ID. Add health endpoints that reveal status only, not configuration or financial data.

- [ ] **Step 5: Run security tests**

Run: `cd backend && ./gradlew test --tests '*Security*'`

Expected: PASS.

- [ ] **Step 6: Add JDBC-backed session storage**

Add Spring Session JDBC using the same PostgreSQL database, manage its schema through the documented Spring Session schema mechanism, set an explicit session timeout, and configure cookie properties for HTTPS production. Test that a session can be read after application context recreation, proving it is not tied to one JVM instance.

- [ ] **Step 7: Commit**

```bash
git add backend
git commit -m "feat: secure api with google oidc"
```

## Task 6: Add Dashboard Queries, Audit, and Cursor Sync

**Files:**
- Create: `backend/src/main/java/com/techie345/moneys/dashboard/...`
- Create: `backend/src/main/java/com/techie345/moneys/audit/...`
- Create: `backend/src/main/java/com/techie345/moneys/sync/...`
- Create: `backend/src/test/java/com/techie345/moneys/dashboard/...`
- Create: `backend/src/test/java/com/techie345/moneys/sync/...`
- Modify: `backend/src/main/resources/db/migration/V1__initial_schema.sql`

- [ ] **Step 1: Write dashboard and sync tests**

Test that dashboard endpoints return calculated summaries from server data, respect date ranges, exclude transfers from spending, preserve privacy-independent numeric values for authorized clients, and return changed records plus a next cursor. Test that every mutation creates an audit event and that cursor results are user-scoped.

- [ ] **Step 2: Implement dashboard query services**

Add `GET /api/v1/dashboard/overview`, `/spending`, `/income`, and `/investing`. Return stable view models containing chart series and summary values so the frontend does not reimplement financial calculations.

- [ ] **Step 3: Implement audit and sync tables/services**

Record owner, operation, resource type, resource ID, source/import ID where applicable, timestamp, and version. Implement `GET /api/v1/sync?cursor=<cursor>` using a monotonic owner-scoped change sequence. Include created/updated/deleted records and a next cursor.

- [ ] **Step 4: Run tests and commit**

Run: `cd backend && ./gradlew test --tests '*Dashboard*' --tests '*Sync*'`

Expected: PASS.

```bash
git add backend
git commit -m "feat: add dashboard queries and sync"
```

## Task 6a: Generate API Documentation from Contract Tests

**Files:**
- Modify: `backend/build.gradle`
- Create: `backend/src/docs/asciidoc/api.adoc`
- Create: `backend/src/test/java/com/techie345/moneys/api/ApiDocumentationTest.java`
- Modify: `README.md`

- [ ] **Step 1: Add REST Docs configuration**

Configure the Gradle Asciidoctor task to consume `build/generated-snippets`, run after tests, and package generated HTML documentation without embedding secrets or real financial records.

- [ ] **Step 2: Document representative API contracts**

Use MockMvc tests with Spring REST Docs for authentication status, account CRUD, transaction validation, dashboard overview, import preview/commit, `409` conflict, and sync cursor responses. Assert status, headers, request fields, response fields, and error fields in the test so undocumented contract changes fail CI.

- [ ] **Step 3: Build documentation**

Run: `cd backend && ./gradlew test asciidoctor`

Expected: PASS and generated documentation under `backend/build/asciidoc/html5`.

- [ ] **Step 4: Commit**

```bash
git add backend README.md
git commit -m "docs: generate api contracts from tests"
```

## Task 7: Convert the Frontend to an API Client

**Files:**
- Create: `src/api/client.ts`
- Create: `src/api/types.ts`
- Create: `src/api/errors.ts`
- Create: `src/api/client.test.ts`
- Modify: `src/App.tsx`
- Modify: `src/data/bootstrap.ts`
- Modify: `src/data/storage.ts`
- Modify: `src/components/pages/*.tsx`
- Modify: `src/components/dashboard/*.tsx`

- [ ] **Step 1: Write failing API client tests**

Test request JSON decoding, credentials inclusion, `204` handling, structured error parsing, `401` session expiration, and `409` conflict responses. Mock `fetch` only at the client boundary.

- [ ] **Step 2: Run focused frontend tests to verify they fail**

Run: `npm test -- --run src/api/client.test.ts`

Expected: FAIL because `src/api/` does not exist.

- [ ] **Step 3: Implement the typed API client**

Create resource methods for `/api/v1`, dashboard queries, imports, and sync. Always send `credentials: 'include'`, accept JSON, map the backend error contract to a typed error, and never calculate financial totals in the client.

- [ ] **Step 4: Replace bootstrap ownership**

On app load, call `/me` and then dashboard/resource endpoints. Keep loading, unauthenticated, and server-error states explicit. Remove automatic demo seeding in authenticated production mode. Retain demo data only as an explicit local fixture/development mode.

- [ ] **Step 5: Convert pages and charts**

Update pages to render backend DTOs and dashboard view models. Preserve responsive desktop sidebar, mobile bottom navigation, accessible headings/labels/focus states, and privacy masking for every displayed amount and chart value.

- [ ] **Step 6: Run frontend tests and type-check**

Run: `npm test -- --run && npx tsc --noEmit`

Expected: PASS, with tests updated to assert API loading and error states instead of local dataset bootstrap.

- [ ] **Step 7: Commit**

```bash
git add src
git commit -m "feat: convert frontend to api client"
```

## Task 8: Move Import UI to Server Preview and Commit

**Files:**
- Modify: `src/components/pages/ImportPage.tsx`
- Modify: `src/import/*.ts`
- Modify: `src/import/*.test.ts`
- Modify: `src/api/client.ts`
- Create: `src/components/pages/ImportPage.integration.test.tsx`

- [ ] **Step 1: Write the failing server-import UI test**

Test file selection, profile selection, upload/preview, display of invalid rows and duplicate candidates, explicit commit confirmation, cancellation, and server errors. Assert no canonical frontend cache update occurs before commit succeeds.

- [ ] **Step 2: Implement server import calls**

Send the source file/profile to `POST /imports`, poll or fetch preview, render all backend validation summaries, and call commit only after confirmation. Remove frontend normalization/commit as the source of truth; retain only presentation helpers needed by the UI.

- [ ] **Step 3: Refresh server state after commit**

After a successful commit, invalidate/refetch affected resources and dashboard queries. Show the backend audit/source metadata in the import result.

- [ ] **Step 4: Run focused and full frontend tests**

Run: `npm test -- --run src/components/pages/ImportPage.integration.test.tsx && npm test -- --run`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src
git commit -m "feat: use server import workflow"
```

## Task 9: Local Deployment, Documentation, and End-to-End Verification

**Files:**
- Create: `docker-compose.yml`
- Create: `backend/.env.example`
- Create: `.env.example`
- Modify: `README.md`
- Create: `e2e/auth-import-sync.spec.ts`
- Modify: `.github/workflows/node-build.yml`
- Create: `.github/workflows/backend-build.yml`

- [ ] **Step 1: Add local Compose services**

Define PostgreSQL health checks, backend dependency ordering, persistent local database volume, backend port, and frontend API URL. Document Google client ID/secret, redirect URI, database URL, and allowed frontend origins using environment variables.

- [ ] **Step 2: Add backend CI**

Run backend unit/integration tests, Testcontainers tests, Gradle `check`, and image build. Keep frontend CI running its existing test, type-check, and build commands.

- [ ] **Step 3: Write end-to-end tests**

Cover authenticated session setup, import preview without persistence, explicit commit, dashboard refresh, and a second browser context reading the same user data. Use test OAuth/session fixtures rather than real Google network calls.

- [ ] **Step 4: Update documentation**

Replace the browser-local-only README claims with the two-application architecture, local Compose commands, API base URL, OAuth setup, canonical backend ownership, cache behavior, supported file sources, and explicit statement that live providers are deferred.

- [ ] **Step 5: Run complete verification**

Run:

```bash
npm test -- --run
npx tsc --noEmit
npm run build
cd backend && ./gradlew test
docker compose config
```

Expected: all tests pass, TypeScript compiles, frontend builds, backend tests pass, and Compose configuration validates.

- [ ] **Step 6: Commit**

```bash
git add docker-compose.yml backend .env.example README.md e2e .github/workflows
git commit -m "ci: add full stack local deployment"
```

## Plan Self-Review

- Spec coverage: architecture and modular monolith are covered by Tasks 1, 1a, and 2-3; provider boundary and import guarantees by Task 4; Google OIDC/privacy and shared sessions by Task 5; API/dashboard/sync by Tasks 3, 6, and 6a; errors and consistency by Tasks 3-4; deployment/testing by Task 9.
- Completeness scan: no unresolved requirements remain. Live institution providers are explicitly out of the first scope and represented by the provider interface.
- Type consistency: the Java provider signatures are reused in Task 4; `/api/v1` and import/sync paths match the API design; frontend client conversion consumes the DTO/view-model endpoints defined in Tasks 3, 4, and 6.
- Scope control: the roadmap is phased, but each task produces a testable backend/frontend increment. Microservice extraction, external bearer-token clients, and live aggregators are intentionally excluded from this implementation cycle.
