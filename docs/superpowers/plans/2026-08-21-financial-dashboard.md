# Financial Dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a polished, browser-local React dashboard for spending, cash, investing, housing, vehicles, income, debt, net worth, and institution-specific CSV imports.

**Architecture:** A Vite React TypeScript single-page app separates domain calculations, storage, CSV parsing, and presentation. IndexedDB stores normalized financial data; localStorage stores preferences and merchant rules. Import adapters normalize institution-specific CSVs into a common model, while dashboard views consume derived selectors. No network calls are made in part 1.

**Tech Stack:** Node 24 LTS, React 19.2, TypeScript 7, Vite 8, Vitest 4, Papa Parse 5.6, IndexedDB via `idb` 8, Recharts 3, and CSS.

---

## File Map

- Create: `package.json`, `index.html`, `vite.config.ts`, `tsconfig.json`, `src/main.tsx`.
- Create: `src/domain/types.ts`, `src/domain/categories.ts`, `src/domain/calculations.ts`, `src/domain/rules.ts`.
- Create: `src/data/demo-data.ts`, `src/data/storage.ts`, `src/data/seed.ts`.
- Create: `src/import/csv.ts`, `src/import/profiles.ts`, `src/import/normalize.ts`, `src/import/dedupe.ts`.
- Create: `src/components/layout/AppShell.tsx`, `src/components/layout/Sidebar.tsx`, `src/components/layout/MobileNav.tsx`.
- Create: `src/components/ui/MetricCard.tsx`, `src/components/ui/SectionHeader.tsx`, `src/components/ui/EmptyState.tsx`, `src/components/ui/PrivacyValue.tsx`.
- Create: `src/components/dashboard/OverviewPage.tsx`, `src/components/dashboard/SpendingChart.tsx`, `src/components/dashboard/NetWorthChart.tsx`, `src/components/dashboard/AccountStrip.tsx`.
- Create: `src/components/pages/SpendingPage.tsx`, `src/components/pages/AccountsPage.tsx`, `src/components/pages/InvestingPage.tsx`, `src/components/pages/PropertyPage.tsx`, `src/components/pages/VehiclesPage.tsx`, `src/components/pages/IncomePage.tsx`, `src/components/pages/ImportPage.tsx`, `src/components/pages/SettingsPage.tsx`.
- Create: `src/App.tsx`, `src/styles/tokens.css`, `src/styles/global.css`.
- Create: `src/domain/calculations.test.ts`, `src/import/normalize.test.ts`, `src/import/dedupe.test.ts`, `src/domain/rules.test.ts`.

### Task 1: Scaffold the React app and test runner

**Files:**
- Create: `package.json`, `index.html`, `vite.config.ts`, `tsconfig.json`, `src/main.tsx`
- Test: `src/smoke.test.ts`

- [ ] **Step 1: Write the failing smoke test**

```ts
import { describe, expect, it } from 'vitest';

describe('test setup', () => {
  it('runs Vitest', () => expect(true).toBe(true));
});
```

- [ ] **Step 2: Pin the current compatible runtime and package versions**

Use Node 24 LTS (`24.19.x` or the current `24.x` security release), not the Node 26 current line. Vite 8 requires Node `^20.19.0 || >=22.12.0`; Node 24 LTS is the stable compatibility target.

Install these versions or newer patch releases within the same major line:

```json
{
  "dependencies": {
    "@vitejs/plugin-react": "^6.1.0",
    "idb": "^8.0.3",
    "papaparse": "^5.6.0",
    "react": "^19.2.8",
    "react-dom": "^19.2.8",
    "recharts": "^3.10.1"
  },
  "devDependencies": {
    "@types/react": "^19.2.18",
    "@types/react-dom": "^19.2.4",
    "@types/papaparse": "^5.5.2",
    "typescript": "^7.0.2",
    "vite": "^8.2.2",
    "vitest": "^4.1.11"
  }
}
```

Use the repository's current lockfile after installation so exact resolved versions are reproducible. If the installed Node runtime is below `20.19`, stop and upgrade it before installing Vite 8.

- [ ] **Step 3: Add the Vite scripts and dependencies**

Add scripts for `dev`, `build`, `test`, and `test:watch`; include the React plugin and the versions listed above.

- [ ] **Step 4: Add the app entry point and minimal root component**

Render `<App />` from `src/main.tsx` and create a temporary root heading in `src/App.tsx`.

- [ ] **Step 5: Run the test and build**

Run: `npm test -- --run` and `npm run build`

Expected: the smoke test passes and Vite produces a production build.

### Task 2: Define domain types, categories, and calculations

**Files:**
- Create: `src/domain/types.ts`, `src/domain/categories.ts`, `src/domain/calculations.ts`
- Test: `src/domain/calculations.test.ts`

- [ ] **Step 1: Write failing calculation tests**

Cover integer-cent totals, transfer exclusion, income versus spending, savings rate, and net worth from cash, investments, assets, and liabilities.

- [ ] **Step 2: Define the shared types**

Define `Account`, `Transaction`, `Asset`, `InvestmentHolding`, `Liability`, `RecurringObligation`, `SavingsGoal`, `MerchantRule`, and `FinancialDataset`. Transactions must include `amountCents`, `kind`, `category`, `source`, `sourceFile`, and `reviewStatus`.

- [ ] **Step 3: Implement pure selectors and calculations**

Implement `sumSpending`, `sumIncome`, `sumTransfers`, `savingsRate`, `netWorthCents`, `monthlySpending`, `spendingByCategory`, and `accountTotals`. Exclude transactions with kind `transfer` or `credit_card_payment` from spending.

- [ ] **Step 4: Run focused tests**

Run: `npm test -- --run src/domain/calculations.test.ts`

Expected: all calculation tests pass.

### Task 3: Add demo data and browser persistence

**Files:**
- Create: `src/data/demo-data.ts`, `src/data/storage.ts`, `src/data/seed.ts`
- Modify: `src/main.tsx`

- [ ] **Step 1: Create realistic fixture data**

Seed Discover, Chase, Amex, SoFi, Fidelity, Mazda Bank, and Newrez accounts with multiple months of transactions, Fidelity holdings, assets, liabilities, income, recurring obligations, and net worth snapshots.

- [ ] **Step 2: Implement IndexedDB storage**

Create database version 1 with one dataset store. Implement `loadDataset`, `saveDataset`, and `clearDataset`. Store preferences and merchant rules in localStorage helpers.

- [ ] **Step 3: Seed on first load**

Load the persisted dataset; if absent, write and return the demo dataset. Expose a loading state to the app rather than rendering partially initialized data.

Validate persisted data before returning it. Reject invalid dates, duplicate IDs, malformed optional collections, and dangling account references. When validation fails, discard the persisted dataset, write a fresh defensive copy of the demo dataset, and render the normal app from that replacement. Add tests for each invalid-data class and verify valid data remains unchanged.

- [ ] **Step 4: Verify persistence manually**

Run `npm run dev`, edit a category in the app after the UI exists, reload, and confirm the edit remains. Clear the data and confirm the demo dataset returns.

### Task 4: Implement deterministic merchant rules

**Files:**
- Create: `src/domain/rules.ts`
- Test: `src/domain/rules.test.ts`

- [ ] **Step 1: Write failing rule tests**

Test case-insensitive merchant matching, first-match precedence, account/type constraints, and preserving manual categories over automatic rules.

- [ ] **Step 2: Implement rule application**

Implement `applyMerchantRules(transactions, rules)` returning updated transactions plus matched rule metadata. Add `normalizeMerchantName` for whitespace, punctuation, and common payment suffixes.

- [ ] **Step 3: Run focused tests**

Run: `npm test -- --run src/domain/rules.test.ts`

Expected: all rule tests pass.

### Task 5: Build CSV parsing and institution profiles

**Files:**
- Create: `src/import/csv.ts`, `src/import/profiles.ts`, `src/import/normalize.ts`, `src/import/dedupe.ts`
- Tests: `src/import/normalize.test.ts`, `src/import/dedupe.test.ts`

- [ ] **Step 1: Write failing normalization tests**

Cover Chase/SoFi checking debit-credit columns, Discover/Amex card charges, Fidelity activity rows, Fidelity positions rows, and mortgage/auto-loan balance rows.

- [ ] **Step 2: Define source profiles**

Each profile must declare institution, source type, recognized headers, account kind, sign convention, and a parser. Include Discover card, Chase card, Amex card, Chase checking, SoFi bank, Fidelity activity, Fidelity positions, Mazda Bank loan, Newrez mortgage, and generic CSV.

- [ ] **Step 3: Implement CSV parsing and profile detection**

Use Papa Parse for header-aware CSV parsing. Detect a profile from normalized headers, expose manual source selection, and return row-level parse errors instead of throwing away the entire file.

- [ ] **Step 4: Implement normalized transaction records**

Normalize dates to ISO dates, amounts to integer cents, merchants/descriptions, direction, account, source metadata, and review status. Keep Fidelity activity as investment activity rather than spending.

- [ ] **Step 5: Write and run dedupe tests**

Use source IDs when present, then fallback to account, date, amount, normalized merchant, and transaction kind. Return duplicate candidates for preview instead of silently dropping them.

Run: `npm test -- --run src/import/normalize.test.ts src/import/dedupe.test.ts`

Expected: all import tests pass.

### Task 6: Build the application shell and visual system

**Files:**
- Create: `src/components/layout/AppShell.tsx`, `src/components/layout/Sidebar.tsx`, `src/components/layout/MobileNav.tsx`
- Create: `src/components/ui/MetricCard.tsx`, `src/components/ui/SectionHeader.tsx`, `src/components/ui/EmptyState.tsx`, `src/components/ui/PrivacyValue.tsx`
- Create: `src/styles/tokens.css`, `src/styles/global.css`
- Modify: `src/App.tsx`

- [ ] **Step 1: Define visual tokens**

Create dark navy surfaces, teal/cyan accents, muted text colors, semantic positive/negative colors, spacing, radii, shadows, and responsive breakpoints.

- [ ] **Step 2: Implement responsive navigation**

Use route-like local state for Overview, Spending, Accounts, Investing, Property, Vehicles, Income, Import, and Settings. Sidebar is visible on desktop; mobile nav replaces it below the mobile breakpoint.

- [ ] **Step 3: Implement shared UI primitives**

Metric cards support labels, values, trend text, loading/empty state, and privacy masking. Use accessible headings, buttons, labels, and focus states.

- [ ] **Step 4: Verify responsive layout**

Run the dev server and inspect at desktop width and a narrow mobile width. Confirm no horizontal overflow and usable keyboard focus order.

### Task 7: Implement overview and financial views

**Files:**
- Create: `src/components/dashboard/OverviewPage.tsx`, `src/components/dashboard/SpendingChart.tsx`, `src/components/dashboard/NetWorthChart.tsx`, `src/components/dashboard/AccountStrip.tsx`
- Create: `src/components/pages/SpendingPage.tsx`, `src/components/pages/AccountsPage.tsx`, `src/components/pages/InvestingPage.tsx`, `src/components/pages/PropertyPage.tsx`, `src/components/pages/VehiclesPage.tsx`, `src/components/pages/IncomePage.tsx`

- [ ] **Step 1: Implement overview metrics from selectors**

Show net worth, cash, income, spending, savings rate, investments, debt, and last-updated dates using demo data and the privacy toggle.

- [ ] **Step 2: Implement charts**

Use Recharts for monthly spending and net worth trends, with accessible summaries and empty states when data is missing.

- [ ] **Step 3: Implement dedicated views**

Render category spending, account balances, investment holdings, housing costs, vehicle costs, salary/other income, recurring obligations, and liabilities using the same domain selectors.

- [ ] **Step 4: Add date-range controls**

Provide 3-month, 6-month, 12-month, and all-time filters. Keep filter state local to the app shell and pass it to view selectors.

### Task 8: Implement import review and export flows

**Files:**
- Create: `src/components/pages/ImportPage.tsx`, `src/components/pages/SettingsPage.tsx`
- Modify: `src/data/storage.ts`, `src/App.tsx`

- [ ] **Step 1: Build upload and source selection**

Accept CSV files, show detected institution/profile, allow correction, and preserve filename/import timestamp.

- [ ] **Step 2: Build mapping and validation preview**

Show detected rows, mapping controls for generic files, parse errors, duplicates, transfers, investment records, uncategorized rows, and rows needing review.

- [ ] **Step 3: Commit normalized data**

On confirmation, merge non-duplicate records into IndexedDB, apply rules, and show a completion summary. Do not mutate persisted data before confirmation.

- [ ] **Step 4: Add JSON/CSV export and clear data**

Export normalized transactions and the full dataset using browser downloads. Add a destructive clear-data confirmation that reseeds demo data after completion.

- [ ] **Step 5: Verify import flows manually**

Test one fixture each for Discover, Chase, Amex, Chase checking, SoFi, Fidelity activity, Fidelity positions, Mazda Bank, Newrez, and generic CSV. Confirm imported records appear in the correct views.

### Task 9: Add verification, polish, and production build checks

**Files:**
- Modify: any files needed for accessibility or test fixes.

- [ ] **Step 1: Run the full test suite**

Run: `npm test -- --run`

Expected: all domain and import tests pass.

- [ ] **Step 2: Run type checking and production build**

Run: `npx tsc --noEmit` and `npm run build`

Expected: no TypeScript errors and a successful Vite build.

- [ ] **Step 3: Check accessibility and responsive behavior**

Verify headings, button labels, keyboard navigation, visible focus, chart summaries, privacy masking, and mobile navigation at narrow width.

- [ ] **Step 4: Check scope boundaries**

Confirm the browser makes no bank, Plaid, AI, or other external data calls and that no credentials are requested.
