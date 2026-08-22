# Task 7 Review Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Correct financial calculations and make every applicable dashboard view date-filtered, privacy-safe, actionable when empty, and accessible.

**Architecture:** Keep financial rules in `src/domain/calculations.ts`; keep shared date filtering and metadata in dashboard view utilities; pass the existing global privacy and date-range state into page/chart components. Use account debt balances as authoritative, and include liabilities only when no corresponding debt account exists.

**Tech Stack:** React 19, TypeScript, Vitest, Recharts, Vite.

---

### Task 1: Calculation and filter contracts

**Files:**
- Modify: `src/domain/calculations.test.ts`
- Modify: `src/components/dashboard/view-utils.ts`
- Modify: `src/components/dashboard/dashboard.test.tsx`

- [x] Add failing tests for debt deduplication, investment account fallback, chronological snapshots, date filtering of transactions, and recurring-obligation selection.
- [x] Run focused domain/dashboard tests and confirm the new assertions fail.
- [x] Implement the smallest calculation/filter helpers and rerun focused tests.

### Task 2: Overview signal

**Files:**
- Modify: `src/components/dashboard/OverviewPage.tsx`
- Modify: `src/components/dashboard/NetWorthChart.tsx`
- Modify: `src/components/dashboard/SpendingChart.tsx`
- Modify: `src/components/ui/EmptyState.tsx`
- Modify: `src/components/dashboard/dashboard.test.tsx`

- [x] Add failing render tests for month-over-month net-worth change, upcoming obligations, masked chart numeric text, masked savings rate, and import/entry empty actions.
- [x] Run the focused dashboard tests and confirm red.
- [x] Add the overview metrics and obligations, filter/sort chart data, route applicable numeric output through `PrivacyValue`, and provide contextual empty-state actions.
- [x] Run focused dashboard tests and confirm green.

### Task 3: Dedicated pages

**Files:**
- Modify: `src/components/pages/PropertyPage.tsx`
- Modify: `src/components/pages/VehiclesPage.tsx`
- Modify: `src/components/pages/SpendingPage.tsx`
- Modify: `src/components/pages/AccountsPage.tsx`
- Modify: `src/components/pages/InvestingPage.tsx`
- Modify: `src/components/pages/IncomePage.tsx`
- Modify: `src/components/pages/page-utils.tsx`
- Modify: `src/components/pages/pages.test.tsx`

- [x] Add failing tests proving property/vehicle costs use the selected date range, spending selectors are applied to cost views, credit-card debt and investment values are represented, and empty views offer relevant actions.
- [x] Run focused page tests and confirm red.
- [x] Apply shared filters and selectors, include the missing account classes in the relevant totals, pass privacy through all numeric values, and add contextual empty states.
- [x] Run focused page tests and confirm green.

### Task 4: Metadata, privacy, responsive/accessibility regression checks

**Files:**
- Modify: `src/components/dashboard/view-utils.ts`
- Modify: `src/components/dashboard/dashboard.test.tsx`
- Modify: `src/components/pages/pages.test.tsx`

- [x] Add focused assertions for meaningful last-updated metadata, accessible chart labels, and preserved range/privacy controls.
- [x] Implement metadata aggregation from account imports, transaction imports, and snapshot dates without mutating source arrays.
- [x] Run the full test suite, `npx tsc --noEmit`, and `npm run build`.
