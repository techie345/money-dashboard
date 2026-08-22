import { describe, expect, it } from 'vitest';
import { createImportedDataset } from './commit';
import type { FinancialDataset } from '../domain/types';

const dataset: FinancialDataset = {
  accounts: [{ id: 'loan', institution: 'Mazda Bank', name: 'Auto Loan', kind: 'loan', balanceCents: -1000 }],
  transactions: [], assets: [], investmentHoldings: [], liabilities: [], recurringObligations: [], savingsGoals: [], merchantRules: [],
};

describe('import dataset commit projection', () => {
  it('persists loan balances in the account, liability, and dated snapshot with source metadata', () => {
    const next = createImportedDataset(dataset, {
      transactions: [], positions: [],
      balances: [{ id: 'mazda-loan-2', accountId: 'loan', date: '2026-08-20', kind: 'auto_loan', balanceCents: 123456, source: 'Mazda Bank', sourceFile: 'loan.csv', importedAt: '2026-08-21T12:00:00.000Z' }],
      errors: [], importedAt: '2026-08-21T12:00:00.000Z',
    }, []);

    expect(next.accounts[0]).toMatchObject({ balanceCents: -123456, source: 'Mazda Bank', sourceFile: 'loan.csv', importedAt: '2026-08-21T12:00:00.000Z' });
    expect(next.liabilities[0]).toMatchObject({ accountId: 'loan', balanceCents: 123456, source: 'Mazda Bank', sourceFile: 'loan.csv', importedAt: '2026-08-21T12:00:00.000Z' });
    expect(next.netWorthSnapshots?.[0]).toMatchObject({ date: '2026-08-20', liabilitiesCents: 123456, source: 'Mazda Bank', sourceFile: 'loan.csv', importedAt: '2026-08-21T12:00:00.000Z' });
  });

  it('uses each loan row balance in its dated historical snapshot', () => {
    const next = createImportedDataset(dataset, {
      transactions: [], positions: [],
      balances: [
        { id: 'mazda-loan-2', accountId: 'loan', date: '2026-07-20', kind: 'auto_loan', balanceCents: 120000, source: 'Mazda Bank' },
        { id: 'mazda-loan-3', accountId: 'loan', date: '2026-08-20', kind: 'auto_loan', balanceCents: 100000, source: 'Mazda Bank' },
      ],
      errors: [],
    }, []);

    expect(next.netWorthSnapshots).toMatchObject([
      { date: '2026-07-20', liabilitiesCents: 120000 },
      { date: '2026-08-20', liabilitiesCents: 100000 },
    ]);
  });
});
