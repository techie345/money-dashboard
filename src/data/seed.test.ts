import { describe, expect, it } from 'vitest';
import { createDemoDataset } from './demo-data';
import { loadOrSeedDataset } from './seed';
import type { FinancialDataset } from '../domain/types';

describe('loadOrSeedDataset', () => {
  it('returns persisted data without replacing it', async () => {
    const persisted: FinancialDataset = {
      ...createDemoDataset(),
      accounts: [],
      transactions: [],
      investmentHoldings: [],
    };
    const storage = {
      loadDataset: async () => persisted,
      saveDataset: async () => undefined,
    };

    await expect(loadOrSeedDataset(storage)).resolves.toBe(persisted);
  });

  it('seeds and persists demo data when no dataset exists', async () => {
    let saved: FinancialDataset | undefined;
    const storage = {
      loadDataset: async () => undefined,
      saveDataset: async (dataset: FinancialDataset) => { saved = dataset; },
    };

    const seeded = await loadOrSeedDataset(storage);
    expect(seeded).toBe(saved);
    expect(seeded).not.toBe(createDemoDataset());
  });

  it('propagates persistence errors so the app can render an error state', async () => {
    const failure = new Error('IndexedDB is unavailable');
    const storage = {
      loadDataset: async () => { throw failure; },
      saveDataset: async () => undefined,
    };

    await expect(loadOrSeedDataset(storage)).rejects.toBe(failure);
  });

  it('replaces malformed persisted data with a fresh demo dataset', async () => {
    const malformed = { accounts: null, transactions: 'not-an-array' } as unknown as FinancialDataset;
    let saved: FinancialDataset | undefined;
    const storage = {
      loadDataset: async () => malformed,
      saveDataset: async (dataset: FinancialDataset) => { saved = dataset; },
    };

    const loaded = await loadOrSeedDataset(storage);

    expect(loaded).not.toBe(malformed);
    expect(loaded.accounts.length).toBeGreaterThan(0);
    expect(saved).toBe(loaded);
  });

  it('replaces persisted data containing invalid dates', async () => {
    const malformed = { ...createDemoDataset(), transactions: [{ ...createDemoDataset().transactions[0], date: 'not-a-date' }] };
    let saved: FinancialDataset | undefined;
    const storage = {
      loadDataset: async () => malformed,
      saveDataset: async (dataset: FinancialDataset) => { saved = dataset; },
    };

    const loaded = await loadOrSeedDataset(storage);

    expect(loaded).not.toBe(malformed);
    expect(saved).toBe(loaded);
  });

  it.each([
    'accounts',
    'transactions',
    'assets',
    'investmentHoldings',
    'liabilities',
    'recurringObligations',
    'savingsGoals',
    'merchantRules',
  ] as const)('replaces persisted data containing duplicate IDs in %s', async (collection) => {
    const dataset = createDemoDataset();
    const malformed = {
      ...dataset,
      [collection]: [dataset[collection][0], dataset[collection][0]],
    } as FinancialDataset;
    const storage = {
      loadDataset: async () => malformed,
      saveDataset: async () => undefined,
    };

    const loaded = await loadOrSeedDataset(storage);

    expect(loaded).not.toBe(malformed);
  });

  it('replaces persisted data containing malformed optional record collections', async () => {
    const malformed = { ...createDemoDataset(), netWorthSnapshots: { date: '2026-08-21' } } as unknown as FinancialDataset;
    const storage = {
      loadDataset: async () => malformed,
      saveDataset: async () => undefined,
    };

    const loaded = await loadOrSeedDataset(storage);

    expect(loaded).not.toBe(malformed);
  });

  it('replaces persisted data containing dangling account references', async () => {
    const dataset = createDemoDataset();
    const malformed = {
      ...dataset,
      transactions: [{ ...dataset.transactions[0], accountId: 'missing-account' }],
      investmentHoldings: [{ ...dataset.investmentHoldings[0], accountId: 'missing-account' }],
    };
    const storage = {
      loadDataset: async () => malformed,
      saveDataset: async () => undefined,
    };

    const loaded = await loadOrSeedDataset(storage);

    expect(loaded).not.toBe(malformed);
  });

  it.each([
    ['merchant rule kind', { kind: 'not-a-kind' }],
    ['transaction sourceId type', { sourceId: 42 }],
    ['transaction sourceId empty', { sourceId: '' }],
    ['transaction categorySource invalid', { categorySource: 'automatic' }],
    ['liability interestRate type', { interestRate: '0.05' }],
    ['liability interestRate negative', { interestRate: -0.01 }],
    ['liability minimumPaymentCents type', { minimumPaymentCents: '100' }],
    ['liability minimumPaymentCents negative', { minimumPaymentCents: -1 }],
    ['transaction amount negative', { amountCents: -1 }],
    ['liability balance negative', { balanceCents: -1 }],
  ] as const)('replaces persisted data with invalid %s', async (field, change) => {
    const dataset = createDemoDataset();
    const malformed = {
      ...dataset,
      merchantRules: [{ ...dataset.merchantRules[0], ...(field === 'merchant rule kind' ? change : {}) }],
      transactions: [{ ...dataset.transactions[0], ...(field.startsWith('transaction ') ? change : {}) }],
      liabilities: [{ ...dataset.liabilities[0], ...(field.startsWith('liability ') ? change : {}) }],
    } as unknown as FinancialDataset;
    const storage = {
      loadDataset: async () => malformed,
      saveDataset: async () => undefined,
    };

    const loaded = await loadOrSeedDataset(storage);

    expect(loaded).not.toBe(malformed);
  });

  it.each([
    ['account id', { id: '' }],
    ['transaction merchant', { merchant: '' }],
    ['liability name', { name: '' }],
  ] as const)('replaces persisted data with an empty required %s', async (field, change) => {
    const dataset = createDemoDataset();
    const malformed = {
      ...dataset,
      accounts: [{ ...dataset.accounts[0], ...(field === 'account id' ? change : {}) }],
      transactions: [{ ...dataset.transactions[0], ...(field === 'transaction merchant' ? change : {}) }],
      liabilities: [{ ...dataset.liabilities[0], ...(field === 'liability name' ? change : {}) }],
    } as unknown as FinancialDataset;
    const storage = {
      loadDataset: async () => malformed,
      saveDataset: async () => undefined,
    };

    const loaded = await loadOrSeedDataset(storage);

    expect(loaded).not.toBe(malformed);
  });
});

describe('demoDataset', () => {
  it('covers the supported institutions and historical records', () => {
    const dataset = createDemoDataset();
    expect(dataset.accounts.map((account) => account.institution)).toEqual(
      expect.arrayContaining(['Discover', 'Chase', 'American Express', 'SoFi', 'Fidelity', 'Mazda Bank', 'Newrez']),
    );
    expect(new Set(dataset.transactions.map((transaction) => transaction.date.slice(0, 7))).size).toBeGreaterThan(1);
    expect(dataset.transactions.some((transaction) => transaction.accountId === 'mazda-loan')).toBe(true);
    expect(dataset.transactions.some((transaction) => transaction.accountId === 'newrez-mortgage')).toBe(true);
    expect(dataset.assets.some((asset) => asset.kind === 'vehicle')).toBe(true);
    expect(dataset.assets.some((asset) => asset.kind === 'property')).toBe(true);
    expect(dataset.investmentHoldings.length).toBeGreaterThan(0);
    expect(dataset.liabilities.some((liability) => liability.kind === 'auto_loan')).toBe(true);
    expect(dataset.liabilities.some((liability) => liability.kind === 'mortgage')).toBe(true);
    expect(dataset.recurringObligations.length).toBeGreaterThan(0);
    expect(dataset.netWorthSnapshots?.length).toBeGreaterThan(1);
  });

  it('returns independent demo dataset instances', () => {
    const first = createDemoDataset();
    const second = createDemoDataset();
    first.accounts[0].name = 'Changed locally';
    first.transactions.pop();
    expect(second.accounts[0].name).not.toBe('Changed locally');
    expect(second.transactions.length).not.toBe(first.transactions.length);
  });
});
