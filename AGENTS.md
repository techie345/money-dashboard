# Agent Instructions

## Project Scope

This is a private, browser-local React and TypeScript application. Do not add backend services, bank integrations, Plaid calls, AI providers, analytics, or credential collection unless the product scope is explicitly changed.

## Development Rules

- Preserve integer cents for monetary values.
- Keep domain calculations pure and independent from React components.
- Validate persisted datasets before passing them to dashboard code.
- Never silently discard invalid CSV rows or duplicate candidates.
- Do not mutate persisted data during import preview; commit only after explicit confirmation.
- Preserve source filename, profile, import timestamp, and source identifiers for auditability.
- Keep Fidelity activity and positions separate from ordinary spending transactions.
- Keep transfer and credit-card-payment records visible but exclude them from spending totals.
- Preserve manually assigned categories when applying merchant rules.
- Maintain the responsive desktop sidebar and mobile bottom navigation.
- Keep privacy masking effective for all displayed monetary values, including charts and summaries.

## Editing and Testing

- Use `apply_patch` for manual file edits.
- Follow test-driven development for behavior changes: write a focused failing test, run it, implement the smallest fix, then run the relevant and full suites.
- Use existing types and selectors before introducing new abstractions.
- Prefer small, focused components and pure helpers.
- Add accessible names, headings, labels, focus states, and table header scopes to UI changes.
- Avoid external network calls in tests and application code.

## Runtime and Packages

- Use Node.js 24 LTS (`>=24.0.0 <26.0.0`).
- Use the versions pinned in `package.json`; update dependency versions deliberately rather than installing incompatible major versions.
- Core runtime packages: React `19.2.8`, React DOM `19.2.8`, Vite React plugin `6.1.0`, `idb` `8.0.3`, Papa Parse `5.6.0`, and Recharts `3.10.1`.
- Development packages: TypeScript `7.0.2`, Vite `8.2.2`, Vitest `4.1.11`, and React type definitions `19.2.x`.
- Run `npm install` before working if dependencies are missing. Do not add packages without a concrete requirement.

## Commands

Run the focused test while developing a behavior change:

```bash
npm test -- --run path/to/file.test.ts
```

Run the complete verification suite before declaring work complete:

```bash
npm test -- --run
npx tsc --noEmit
npm run build
```

Start the local app with:

```bash
npm run dev
```

Use `npm run test:watch` for an interactive Vitest session. The production build runs TypeScript compilation and Vite bundling.

## Verification

Run the following before declaring work complete:

```bash
npm test -- --run
npx tsc --noEmit
npm run build
```

Also inspect responsive behavior at desktop and narrow mobile widths when changing layout, navigation, charts, or import forms.

## Important Paths

- `src/App.tsx`: route-like application state and dataset wiring
- `src/domain/`: financial models and calculations
- `src/data/`: persistence, validation, seed, and demo data
- `src/import/`: CSV parsing and normalization
- `src/components/`: UI and pages
- `docs/superpowers/specs/`: product requirements
- `docs/superpowers/plans/`: implementation plan
