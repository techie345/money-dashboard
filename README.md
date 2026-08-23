# Interactive Moneys

Interactive Moneys is a browser-local personal finance dashboard. It summarizes spending, cash, investments, housing, vehicles, income, debt, and net worth from seeded demo data or imported CSV files.

## Features

- Signal & Control dark dashboard with responsive desktop and mobile navigation
- Local IndexedDB storage for the normalized dataset
- localStorage preferences and merchant rules
- Seeded data for Discover, Chase, American Express, SoFi, Fidelity, Mazda Bank, and Newrez
- Institution-specific and generic CSV import profiles
- Import preview with mapping, validation, transfer, investment, review, and duplicate summaries
- JSON and CSV export
- Privacy masking and clear-and-reseed controls
- No backend, bank connection, Plaid, AI, or credential collection

## Requirements

- Node.js 24 LTS
- npm

## Development

```bash
npm install
npm run dev
```

Open the local URL printed by Vite. The first load seeds a demo dataset in the browser.

## Commands

```bash
npm test -- --run   # Run the full Vitest suite
npm run build       # Type-check and create a production build
npm run dev         # Start the Vite development server
npm run test:watch  # Run Vitest in watch mode
```

## Project Structure

- `src/domain/` contains shared types, categories, calculations, and merchant rules.
- `src/data/` contains demo data, IndexedDB/localStorage access, validation, and bootstrap logic.
- `src/import/` contains CSV parsing, source profiles, normalization, correction, and deduplication.
- `src/components/` contains the application shell, dashboard charts, pages, and UI primitives.
- `src/styles/` contains design tokens and global responsive styles.
- `docs/superpowers/` contains the product design and implementation plan.

## CI and Automation

The project includes GitHub Actions workflows for pushes and pull requests. The CI workflow installs dependencies, runs the full test suite, type-checks the app, and builds the production bundle.

The backend API reference is generated from MockMvc contract tests with Spring REST Docs. Run `JAVA_HOME=/usr/lib/jvm/java-21-openjdk gradle test asciidoctor` from `backend`; the generated HTML is written to `backend/build/docs/asciidoc/api.html` and packaged into the backend JAR under `static/docs/`.

## Data and Privacy

All financial data stays in the browser. Transactions and account data are stored in IndexedDB under the `interactive-moneys` database. Preferences and merchant rules use localStorage. Imported files are parsed locally and are not uploaded.

Use **Settings** to export the full dataset or normalized transactions. **Clear all data** removes local data and reseeds the demo dataset after confirmation.

## Import Flow

1. Select a CSV file.
2. Confirm or change the detected source profile.
3. Correct generic mappings or invalid rows when needed.
4. Review validation, duplicate, transfer, investment, and review counts.
5. Confirm the commit.

No persisted data is changed until the final confirmation.

## Testing Guidance

Domain calculations, persistence validation, merchant rules, CSV normalization, and deduplication have automated coverage. When changing behavior, add a focused test first, run it to confirm the expected failure, then implement the smallest change that makes it pass.

Before submitting changes, run:

```bash
npm test -- --run
npm run build
```
