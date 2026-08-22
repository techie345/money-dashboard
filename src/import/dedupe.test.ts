import { describe, expect, it } from 'vitest';
import { findDuplicateCandidates, findIncomingDuplicateIds, upsertHoldings } from './dedupe';
import type { InvestmentHolding, Transaction } from '../domain/types';

const transaction = (overrides: Partial<Transaction> = {}): Transaction => ({
  id: 'new-1', date: '2026-08-20', merchant: 'Whole Foods POS', description: 'Whole Foods POS', amountCents: 1234,
  kind: 'spending', category: 'food', accountId: 'checking', source: 'Chase', sourceFile: 'x.csv', reviewStatus: 'needs_review', sourceId: 'source-1', ...overrides,
});

describe('duplicate candidates', () => {
  it('matches source IDs before using account, date, amount, normalized merchant, and kind', () => {
    const incoming = [transaction(), transaction({ id: 'new-2', sourceId: undefined, merchant: 'Whole Foods' })];
    const existing = [transaction({ id: 'old-1' }), transaction({ id: 'old-2', sourceId: 'other', merchant: 'Whole Foods Market' })];
    const candidates = findDuplicateCandidates(incoming, existing);

    expect(candidates).toEqual([
      { incomingId: 'new-1', existingId: 'old-1', reason: 'source_id' },
      { incomingId: 'new-2', existingId: 'old-1', reason: 'account_date_amount_merchant_kind' },
    ]);
  });

  it('reports candidates without dropping either row, including ambiguous matches', () => {
    const incoming = [transaction({ sourceId: undefined })];
    const existing = [transaction({ id: 'old-1', sourceId: undefined }), transaction({ id: 'old-2', sourceId: undefined })];
    expect(findDuplicateCandidates(incoming, existing)).toHaveLength(2);
  });

  it('does not match identical source IDs across accounts', () => {
    const incoming = [transaction({ sourceId: 'same-id', accountId: 'checking' })];
    const existing = [transaction({ id: 'old-1', sourceId: 'same-id', accountId: 'savings' })];

    expect(findDuplicateCandidates(incoming, existing)).toEqual([]);
  });

  it('uses value fallback when either source ID is missing but not when IDs conflict', () => {
    const incoming = [transaction({ sourceId: undefined })];
    const existing = [
      transaction({ id: 'old-missing', sourceId: undefined }),
      transaction({ id: 'old-different', sourceId: 'different' }),
    ];

    expect(findDuplicateCandidates(incoming, existing)).toEqual([
      { incomingId: 'new-1', existingId: 'old-missing', reason: 'account_date_amount_merchant_kind' },
      { incomingId: 'new-1', existingId: 'old-different', reason: 'account_date_amount_merchant_kind' },
    ]);

    expect(findDuplicateCandidates(
      [transaction({ sourceId: 'incoming-id' })],
      [transaction({ id: 'old-conflict', sourceId: 'existing-id' })],
    )).toEqual([]);
  });
});

describe('incoming duplicate handling', () => {
  it('detects duplicate rows within the incoming file', () => {
    const incoming = [transaction({ id: 'new-1', sourceId: undefined }), transaction({ id: 'new-2', sourceId: undefined })];
    expect(findIncomingDuplicateIds(incoming)).toEqual(new Set(['new-2']));
  });

  it('detects value duplicates when only one incoming row has a source ID', () => {
    const incoming = [transaction({ id: 'new-1', sourceId: 'source-1' }), transaction({ id: 'new-2', sourceId: undefined })];
    expect(findIncomingDuplicateIds(incoming)).toEqual(new Set(['new-2']));
  });

  it('upserts holdings by account and symbol instead of appending copies', () => {
    const existing: InvestmentHolding[] = [{ id: 'holding-1', accountId: 'brokerage', symbol: 'VTI', shares: 1, costBasisCents: 100, marketValueCents: 200 }];
    const incoming: InvestmentHolding[] = [{ id: 'fidelity-2', accountId: 'brokerage', symbol: 'VTI', shares: 2, costBasisCents: 300, marketValueCents: 500 }];
    expect(upsertHoldings(existing, incoming)).toEqual([{ ...incoming[0], id: 'holding-1' }]);
  });
});
