# Audit and Cursor Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add owner-scoped audit/change events and cursor-based synchronization to the existing Spring backend without changing frontend code or existing response behavior.

**Architecture:** Add a Flyway `V9` `change_event` table with a per-owner monotonic sequence and JSON record snapshot. A focused audit service writes created, updated, and deleted events transactionally with financial and import mutations. A sync controller reads events after the authenticated owner’s cursor, returns ordered changes and the last sequence as `nextCursor`.

**Tech Stack:** Java 21, Spring Boot, Spring MVC, Spring Data JPA, PostgreSQL, Flyway, JUnit/MockMvc.

---

### Task 1: Define schema and audit journal

**Files:**
- Create: `backend/src/main/resources/db/migration/V9__audit_change_events.sql`
- Create: `backend/src/main/java/com/techie345/moneys/audit/ChangeOperation.java`
- Create: `backend/src/main/java/com/techie345/moneys/audit/ChangeEventEntity.java`
- Create: `backend/src/main/java/com/techie345/moneys/audit/ChangeEventRepository.java`
- Create: `backend/src/main/java/com/techie345/moneys/audit/AuditService.java`
- Test: `backend/src/test/java/com/techie345/moneys/audit/AuditServiceTest.java`

- [x] Write tests asserting event fields, owner-scoped sequence ordering, and deleted tombstones.
- [x] Run `gradle test --tests '*AuditServiceTest'` and verify failure because the journal types do not exist.
- [x] Add `change_event` with `(owner_id, sequence)` uniqueness, operation/resource/source/import/timestamp/version columns, and JSON payload; map it with JPA and implement transactional writes.
- [x] Run the focused audit tests and verify they pass.

### Task 2: Add sync read model and endpoint

**Files:**
- Create: `backend/src/main/java/com/techie345/moneys/sync/SyncResponse.java`
- Create: `backend/src/main/java/com/techie345/moneys/sync/SyncService.java`
- Create: `backend/src/main/java/com/techie345/moneys/sync/SyncController.java`
- Test: `backend/src/test/java/com/techie345/moneys/sync/SyncControllerTest.java`

- [x] Write MockMvc/service tests for empty cursors, ordered paging, created/updated/deleted records, invalid cursors, and cross-owner isolation.
- [x] Run `gradle test --tests '*SyncControllerTest'` and verify failure.
- [x] Implement `GET /api/v1/sync?cursor=<cursor>` using `CurrentUser`, `sequence > cursor`, owner filtering, deterministic ordering, and `nextCursor` equal to the final returned sequence or the requested cursor when empty.
- [x] Run focused sync tests and verify they pass.

### Task 3: Wire existing mutations

**Files:**
- Modify: `backend/src/main/java/com/techie345/moneys/financial/FinancialMutationService.java`
- Modify: `backend/src/main/java/com/techie345/moneys/financial/api/FinancialResourceController.java`
- Modify: `backend/src/main/java/com/techie345/moneys/imports/ImportService.java`
- Create/modify: `backend/src/test/java/com/techie345/moneys/financial/FinancialMutationAuditTest.java`
- Modify: `backend/src/test/java/com/techie345/moneys/imports/ImportServiceTest.java`

- [x] Add failing tests proving representative account/transaction create, update, delete, import commit, and discard produce events with owner/resource/version metadata while preview does not.
- [x] Run those focused tests and verify failure.
- [x] Inject `AuditService` into mutation paths; serialize safe resource snapshots, pass import IDs for import-generated events, and write delete tombstones before deletion. Keep all lookups and event queries owner-scoped.
- [x] Run financial and import focused tests and verify existing behavior remains unchanged.

### Task 4: Full verification

- [x] Run `gradle test --tests '*Audit*' --tests '*Sync*' --tests '*Financial*' --tests '*Import*`.
- [ ] Run the complete backend test suite with `gradle test` (blocked by pre-existing session/OIDC context failures).
- [ ] Run `npx tsc --noEmit` and `npm run build` only as non-modifying regression checks; do not edit frontend files.
- [x] Inspect `git diff` and `git status --short`; do not commit.
