# Task 4 Review Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Preserve category provenance, make merchant rule matching safe and deterministic, and expose rule application for dataset/import integration.

**Architecture:** Add optional `Transaction.categorySource` for legacy compatibility. Centralize merchant and rule normalization in `src/domain/rules.ts`; matching uses normalized literal alternatives rather than unsafe dynamic regular expressions. Expose both transaction-level and dataset-level pure rule application APIs, and validate/demo-seed provenance fields.

**Tech Stack:** TypeScript, Vitest, Vite.

---

### Task 1: Provenance and validation tests

**Files:**
- Modify: `src/domain/rules.test.ts`
- Modify: `src/data/seed.test.ts`

- [ ] Write failing tests for manual provenance skipping, matched records receiving `categorySource: 'rule'`, legacy records without the field remaining valid, and invalid provenance being rejected.
- [ ] Run focused tests and observe the expected failures.

### Task 2: Safe, consistent rule matching

**Files:**
- Modify: `src/domain/rules.test.ts`
- Modify: `src/domain/rules.ts`

- [ ] Add failing tests for payment prefixes/suffixes, numeric IDs, equivalent merchant/rule normalization, punctuation-only patterns, and literal metacharacters.
- [ ] Implement shared normalization, usable-pattern filtering, literal alternative matching, and provenance updates.
- [ ] Run the focused rule tests to green.

### Task 3: Dataset integration API and fixture updates

**Files:**
- Modify: `src/domain/rules.test.ts`
- Modify: `src/domain/rules.ts`
- Modify: `src/domain/types.ts`
- Modify: `src/data/seed.ts`
- Modify: `src/data/demo-data.ts`

- [ ] Add a failing dataset-level integration test proving only transactions are rule-applied and source metadata is preserved.
- [ ] Implement `applyMerchantRulesToDataset` and optional provenance validation.
- [ ] Mark demo transactions as imported and verify seed validation.
- [ ] Run all tests, typecheck, and production build.
