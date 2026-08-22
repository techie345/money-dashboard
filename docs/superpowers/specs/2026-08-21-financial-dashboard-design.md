# Financial Dashboard Part 1 Design

## Goal

Build a private, browser-local financial dashboard that turns manually maintained JSON data and exported CSV files into a useful overview of spending, cash, investing, housing, vehicle costs, income, debt, and net worth.

Part 1 intentionally excludes bank connections and AI categorization. AI and Plaid Enrich are follow-up part 2 integrations behind an adapter boundary.

## Product Direction

The visual direction is **Signal & Control**: a dark canvas, bright teal/cyan data accents, dense but readable charts, and a personal financial command-center feel.

The app must work well on desktop and mobile. Desktop uses sidebar navigation; mobile uses bottom navigation. Sensitive values can be hidden with a privacy toggle.

## Technology and Storage

- React, Vite, and TypeScript.
- Browser-only storage for part 1.
- IndexedDB for transaction and account data.
- localStorage for small preferences and saved merchant rules.
- No backend, bank credentials, or external API calls in part 1.
- Seeded demo dataset so the app is useful immediately.
- Export current normalized data as JSON or CSV through an explicit browser download.
- Clear-all-data action in settings.

## Account Configuration

The seeded account configuration includes:

- Credit cards: Discover, Chase, American Express.
- Bank accounts: Chase, SoFi.
- Investing: Fidelity.
- Auto loan: Mazda Bank.
- Mortgage: Newrez.

Users can add generic accounts later without code changes. Each account stores institution, nickname, account type, last imported date, balance/value, import format, visibility, and optional manual adjustment.

## Data Model

Core records:

- `accounts`: financial account identity and current balance/value.
- `transactions`: date, merchant, description, amount in integer cents, account, category, type, source, review status, transfer metadata, and source linkage.
- `assets`: property, vehicles, and other manually valued assets.
- `income`: salary and other recurring income.
- `investmentHoldings`: symbol, shares, cost basis, current value, and allocation.
- `liabilities`: mortgage, auto loan, student loan, credit card balance, interest rate, and minimum payment.
- `recurringObligations`: rent/mortgage, utilities, subscriptions, insurance, and debt payments.
- `savingsGoals`: target, current amount, deadline, and progress.
- `rules`: merchant pattern, category, optional account/type constraints.
- `netWorthSnapshots`: dated asset and liability totals for historical tracking.
- `settings`: currency, date range, taxonomy, and display preferences.

Transaction and account source metadata must include institution, source type, original filename, import timestamp, and source identifier when available.

Persisted datasets must be structurally validated before use. Validation must reject invalid dates, duplicate record IDs, malformed optional record collections, and references to missing accounts. Invalid persisted data must be discarded and replaced with a fresh demo dataset rather than passed to dashboard code.

## Dashboard

The overview shows:

- Net worth and month-over-month change.
- Cash position.
- Income versus spending.
- Savings rate.
- Investment value.
- Debt balance.
- Monthly spending trend.
- Upcoming recurring obligations.
- Last-updated dates for accounts and major metrics.

Dedicated sections:

- Spending.
- Cash.
- Investing.
- Housing.
- Vehicles.
- Income.
- Net worth.
- Import/export.

Charts and summaries support date-range filtering. Empty states explain what data is missing and link to the relevant import or entry action.

## CSV Import

The import flow is `Upload -> Source selection -> Map/validate -> Preview -> Commit`.

Supported source profiles:

- Discover credit card.
- Chase credit card.
- American Express credit card.
- Chase checking.
- SoFi bank account.
- Fidelity brokerage activity.
- Fidelity positions.
- Mazda Bank auto loan.
- Newrez mortgage.
- Generic bank/card CSV fallback.

Institution-specific profiles map known columns and sign conventions. The generic flow detects likely date, merchant, description, amount, debit, credit, and account columns and lets the user correct mappings.

Fidelity brokerage activity remains separate from spending and supports buys, sells, dividends, fees, deposits, and withdrawals. Fidelity positions support holdings, shares, cost basis, market value, and allocation.

Bank and card imports normalize spending and cash-flow transactions. Credit card payments and account transfers are detected and excluded from spending totals while remaining visible as transfers.

The importer must:

- Normalize dates and amounts.
- Detect likely transfers.
- Detect duplicate candidates.
- Apply saved merchant rules.
- Mark unknown or ambiguous rows as `Needs review`.
- Preserve source file and import metadata.
- Show a validation summary before commit.
- Never silently discard duplicate candidates or invalid rows.

Validation summary includes recognized rows, rows needing mapping, duplicate candidates, transfers detected, investment records detected, and uncategorized transactions.

## Calculations and Integrity

- Store monetary values as integer cents.
- Spending is categorized outflow excluding transfers and credit card payments.
- Income is categorized inflow.
- Account balances are independent from transaction totals.
- Fidelity, mortgage, and car loan values support manual balance snapshots.
- Net worth equals cash plus investments plus property value plus vehicle value minus credit cards and loans.
- Incomplete data is flagged instead of presented as false precision.
- Original source records remain linked to normalized records so imports can be audited or removed.

## Categorization and Part 2 Boundary

Part 1 uses built-in categories, editable merchant rules, and manual category editing. Corrections save as future merchant rules.

Part 2 may add:

- Plaid Enrich for merchant, category, location, payment channel, and website enrichment from imported transactions.
- AI fallback for ambiguous transactions and custom categories.
- A review queue showing suggested category, confidence, rationale, and accept/edit actions.

The part 1 importer and normalized transaction model must expose an adapter boundary so these providers can be added without changing dashboard components. No transaction data leaves the device in part 1.

## Privacy and Error Handling

- All part 1 financial data remains in the browser.
- Unsupported formats fall back to generic mapping.
- Parsing errors identify affected rows and suggest fixes.
- Invalid dates, amounts, and missing account assignments block commit.
- Duplicate imports require preview and confirmation.
- Users can clear all local data.
- Corrupt or incompatible persisted datasets are rejected, cleared, and reseeded safely.

## Verification

Tests should cover:

- CSV parsing and column mapping.
- Institution-specific sign normalization.
- Duplicate detection.
- Merchant rules.
- Transfer and credit-card-payment exclusion.
- Net worth calculations.
- Fidelity activity and positions parsing.
- Responsive navigation.

Fixture data should include spending, income, transfers, investments, loans, recurring obligations, and multiple institution formats. The seeded demo dataset should support visual and manual verification of every primary dashboard view.

## Scope Boundaries

Included in part 1:

- Browser-local React dashboard.
- Seeded JSON data.
- Institution-specific CSV imports listed above.
- Generic CSV fallback.
- Deterministic categorization and manual review.
- Spending, cash, investing, housing, vehicles, income, debt, and net worth views.
- JSON and CSV export.

Excluded from part 1:

- Live bank or brokerage connections.
- Plaid Enrich.
- AI categorization.
- User accounts or cloud sync.
- Multi-user access.
