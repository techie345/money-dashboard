import { describe, expect, it } from 'vitest';
import { transactionsCsv } from './SettingsPage';
import type { FinancialDataset } from '../../domain/types';

const dataset = { transactions: [{ date: '=2+2', merchant: '+SUM(A1:A2)', description: '-danger', amountCents: 1, kind: 'spending', category: 'other', accountId: 'a', source: 'x', sourceFile: 'x', reviewStatus: 'needs_review', id: 't' }] } as FinancialDataset;

describe('transaction CSV export', () => {
  it('neutralizes spreadsheet formulas in exported cells', () => {
    const csv = transactionsCsv(dataset);
    expect(csv).toContain('"\'=2+2"');
    expect(csv).toContain('"\'+SUM(A1:A2)"');
    expect(csv).toContain('"\'-danger"');
  });
});
