# Interactive Moneys Backend

Spring Boot backend for Interactive Moneys. The backend is the canonical owner of users, financial records, imports, calculations, audit events, and synchronization. The React application is a client of this API and must not reimplement server calculations or import normalization.

## Requirements

- Java 21
- Gradle 9.x
- PostgreSQL 16 or compatible
- Docker or Podman for Testcontainers integration tests

The repository currently uses the system `gradle` command. A Gradle wrapper can be generated with `gradle wrapper` when wrapper files are added to the project.

## Run Locally

Create a PostgreSQL database and provide its connection settings:

```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/moneys
export DATABASE_USERNAME=moneys
export DATABASE_PASSWORD=change-me
export CORS_ALLOWED_ORIGINS=http://localhost:5173
```

Start the application from this directory:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk \
PATH=/usr/lib/jvm/java-21-openjdk/bin:$PATH \
gradle bootRun
```

The API listens on port `8080`. Management endpoints listen on loopback port `8081` by default. Flyway owns schema creation and migrations, including Spring Session JDBC tables.

## Configuration

Configuration is supplied through environment-backed Spring properties. Important settings include:

| Variable | Default | Purpose |
| --- | --- | --- |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/moneys` | PostgreSQL JDBC URL |
| `DATABASE_USERNAME` | `moneys` | Database user |
| `DATABASE_PASSWORD` | empty | Database password |
| `SERVER_PORT` | `8080` | API port |
| `MANAGEMENT_PORT` | `8081` | Actuator port |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Allowed browser origins |
| `SESSION_SECURE_COOKIE` | `false` | Set `true` behind HTTPS |
| `SESSION_TIMEOUT` | `30m` | Session lifetime |
| `MAX_UPLOAD_BYTES` | `10485760` | Import upload limit |

OAuth client credentials are deployment secrets and must not be committed. Configure the Google OAuth2 client and callback settings in the deployment environment.

## Test

Run the complete backend suite with Java 21:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk \
PATH=/usr/lib/jvm/java-21-openjdk/bin:$PATH \
gradle clean test
```

The repository and session tests use PostgreSQL through Testcontainers. Rootless Podman can be used by configuring Testcontainers' Docker socket in the local environment.

Build the executable jar with:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk \
PATH=/usr/lib/jvm/java-21-openjdk/bin:$PATH \
gradle build
```

The jar is written to `build/libs/money-backend.jar`.

## API Areas

All application endpoints are versioned under `/api/v1`.

- Identity: `/me`, `/profile`
- Financial resources: accounts, transactions, assets, holdings, liabilities, obligations, goals, rules, and net-worth snapshots
- Dashboard: overview, spending, income, and investing
- Imports: create, preview, commit, and discard
- Synchronization: `/sync?cursor=<cursor>&limit=<limit>`
- Health and metrics: `/actuator/health`, `/actuator/info`, and `/actuator/metrics` on the management interface

Mutations require an authenticated browser session and CSRF protection. Stale `If-Match` writes return `409 Conflict`. All records and sync events are scoped to the authenticated user.

## Security Boundaries

- Google OIDC identifies users by verified subject, not email alone.
- Browser sessions use secure, HttpOnly, SameSite cookies in production.
- Google tokens are kept server-side and are never returned to React.
- No bank credentials, Plaid integration, analytics, or external provider calls are included.
- Financial values remain integer cents; privacy masking is a frontend display concern, not an authorization boundary.

## Project Layout

- `src/main/java/com/techie345/moneys/identity`: OIDC, sessions, user context, and profile
- `src/main/java/com/techie345/moneys/financial`: resources, validation, mutations, and calculations
- `src/main/java/com/techie345/moneys/imports`: providers and server-owned import workflow
- `src/main/java/com/techie345/moneys/audit`: mutation audit journal and snapshots
- `src/main/java/com/techie345/moneys/sync`: owner-scoped cursor synchronization
- `src/main/resources/db/migration`: Flyway schema migrations
