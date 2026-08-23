# Finance API Backend Design

## Goal

Transform Interactive Moneys from a browser-local dashboard into a full finance application with independently deployable frontend and backend applications. The backend becomes the canonical source of truth so the same user can access the same data from multiple devices.

The first source scope includes CSV/OFX/JSON imports and manual entry. Live institution connections are intentionally deferred, but the backend defines a provider boundary for adding them later.

## Architecture

The system consists of a React frontend, a Spring Boot backend, and PostgreSQL:

```text
React frontend
  - Authentication redirect and session-aware API client
  - Routing, forms, tables, charts, loading/error states
  - No financial calculations or import normalization
        |
        | HTTPS, secure session cookie, JSON REST API
        v
Spring Boot backend
  - Google OIDC login and secure session management
  - Canonical user-owned financial data
  - PostgreSQL persistence and migrations
  - REST resources and dashboard query endpoints
  - Import parsing, validation, preview, deduplication, and commit
  - Categorization rules, calculations, audit metadata, and sync
        |
        v
PostgreSQL
```

The backend is a modular monolith, not a collection of microservices. Initial internal modules are identity, users, financial resources, imports, calculations, synchronization, and audit. Each module has explicit controllers, API DTOs, application services, domain rules, repositories, and tests. Modules do not reach into one another's repositories or persistence entities.

The frontend may retain a small IndexedDB cache for responsiveness and temporary UI state, but the backend is authoritative. The backend returns DTOs and view models rather than database entities.

## Provider Boundary

Providers translate source-specific input into a canonical import model. Providers do not write directly to the database; the import application service owns validation, deduplication, categorization, audit metadata, and persistence.

```java
public interface FinancialDataProvider {
    ProviderType type();
    ProviderMetadata metadata();

    ProviderPreview preview(ProviderInput input, ProviderContext context);

    NormalizedImport normalize(ProviderInput input, ProviderContext context);
}
```

Initial implementations are `GenericCsvProvider`, institution-specific CSV providers, `JsonBackupProvider`, and `ManualEntryProvider`. Future implementations may include Plaid, FDX, or brokerage API adapters. Live providers should implement the common provider port directly rather than inherit from a file-specific base class.

A narrowly scoped `AbstractFileProvider` may hold genuinely common file behavior such as header and text normalization. It must not own persistence or be required by live providers.

The import pipeline is:

```text
Provider input
  -> source parsing and provider normalization
  -> validation
  -> duplicate analysis
  -> categorization and rules
  -> preview
  -> explicit commit
  -> audit record
```

## API

The REST API is versioned under `/api/v1` and exposes resource-oriented endpoints:

- `GET /me`
- `GET/PATCH /profile`
- `GET/POST/PATCH/DELETE /accounts`
- `GET/POST/PATCH/DELETE /transactions`
- `GET/POST/PATCH/DELETE /assets`
- `GET/POST/PATCH/DELETE /investment-holdings`
- `GET/POST/PATCH/DELETE /liabilities`
- `GET/POST/PATCH/DELETE /recurring-obligations`
- `GET/POST/PATCH/DELETE /savings-goals`
- `GET/POST/PATCH/DELETE /merchant-rules`
- `GET /net-worth-snapshots`
- `GET /dashboard/overview`
- `GET /dashboard/spending`
- `GET /dashboard/income`
- `GET /dashboard/investing`

Import endpoints are:

- `POST /imports` to create an import and upload source data
- `GET /imports/{id}/preview` to retrieve validation, duplicate, categorization, transfer, and review results
- `POST /imports/{id}/commit` to commit after explicit confirmation
- `DELETE /imports/{id}` to discard an uncommitted import

All records are scoped to the authenticated user. Monetary values remain integer cents. Transfers and credit-card-payment records remain visible while excluded from spending calculations. Source filename, profile, import timestamp, and source identifiers are retained for auditability.

Synchronization initially uses a cursor:

```text
GET /api/v1/sync?cursor=<cursor>
```

The response contains changed records and a next cursor. Mutations include a server version or updated timestamp. Stale writes return `409 Conflict` with the current server representation rather than silently overwriting data.

## Authentication and Privacy

The browser uses backend-managed Google OIDC:

```text
Frontend -> backend /oauth2/authorization/google
Google -> backend callback
Backend -> secure HttpOnly session cookie
Frontend -> authenticated /api/v1/* requests
```

Spring Security owns OAuth2 login and callback handling. Users are identified by the verified Google subject identifier, not email alone. Sessions use secure, HttpOnly, SameSite cookies. CORS allows only configured frontend origins, and CSRF protection remains enabled for cookie-authenticated browser mutations.

External non-browser clients will receive a separately designed bearer-token or API-key mechanism later; they do not reuse browser session cookies. Google access tokens are not exposed to React. No bank credentials are stored by this application.

Production secrets come from deployment configuration. Sensitive operations and imports create audit records. The frontend privacy mask remains a display preference, not a security boundary; authorized financial values are transmitted over HTTPS.

## Errors and Consistency

Errors use a stable JSON contract:

```json
{
  "status": 422,
  "code": "IMPORT_VALIDATION_FAILED",
  "message": "The import contains invalid records.",
  "details": [],
  "traceId": "..."
}
```

Invalid rows and duplicate candidates are reported, never silently discarded. Import preview does not mutate canonical data. Commit is transactional and idempotent. Validation failures use `400` or `422`, missing resources use `404`, stale writes use `409`, and unexpected failures use a safe `500` response without financial data leakage.

## Deployment and Testing

The frontend is deployed independently as a static or frontend-hosted application. The backend is containerized and connects to PostgreSQL. Docker Compose provides local backend and database development. Frontend API URLs and backend CORS origins are environment-specific. Flyway manages schema migrations.

Testing includes pure domain tests for calculations, categorization, deduplication, and normalization; Spring MVC contract and authorization tests; PostgreSQL repository tests; import preview/commit integration tests; OAuth security tests; frontend API-state tests; and end-to-end tests for login, import, sync, and multi-device behavior.

The initial backend remains one deployable Spring Boot application plus PostgreSQL. Internal module boundaries and architecture tests keep later extraction possible, but services are extracted only when independent scaling, release cadence, isolation, or data ownership provides a concrete reason.
