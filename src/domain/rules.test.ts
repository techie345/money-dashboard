import { describe, expect, it } from 'vitest';
import { applyMerchantRules, applyMerchantRulesToDataset, normalizeMerchantName } from './rules';
import type { FinancialDataset, MerchantRule, Transaction } from './types';

const transaction = (overrides: Partial<Transaction> = {}): Transaction => ({
  id: 'tx-1',
  date: '2026-08-01',
  merchant: 'Whole Foods Market',
  description: 'Whole Foods Market',
  amountCents: 1250,
  kind: 'spending',
  category: 'other',
  accountId: 'checking',
  source: 'Chase',
  sourceFile: 'august.csv',
  reviewStatus: 'needs_review',
  ...overrides,
});

const rule = (overrides: Partial<MerchantRule> = {}): MerchantRule => ({
  id: 'food-rule',
  pattern: 'whole foods',
  category: 'food',
  ...overrides,
});

describe('normalizeMerchantName', () => {
  it('normalizes whitespace, punctuation, and common payment suffixes', () => {
    expect(normalizeMerchantName('  SQ *Whole-Foods Market, INC.  POS  ')).toBe('whole foods market');
    expect(normalizeMerchantName('Trader Joes   #1234')).toBe('trader joes');
    expect(normalizeMerchantName('PAYPAL *NETFLIX 123456789')).toBe('netflix');
  });
});

describe('applyMerchantRules', () => {
  it('matches merchants case-insensitively and returns the matched rule metadata', () => {
    const result = applyMerchantRules([transaction()], [rule()]);

    expect(result.transactions[0].category).toBe('food');
    expect(result.matchedRules).toEqual({ 'tx-1': rule() });
  });

  it('uses the first matching rule', () => {
    const result = applyMerchantRules([transaction()], [
      rule({ id: 'general', pattern: 'whole', category: 'shopping' }),
      rule({ id: 'specific', pattern: 'whole foods', category: 'food' }),
    ]);

    expect(result.transactions[0].category).toBe('shopping');
    expect(result.matchedRules['tx-1'].id).toBe('general');
  });

  it('requires account and transaction kind constraints when present', () => {
    const result = applyMerchantRules([transaction({ accountId: 'card', kind: 'income' })], [
      rule({ accountId: 'checking', kind: 'spending' }),
      rule({ id: 'income-rule', accountId: 'card', kind: 'income', category: 'salary' }),
    ]);

    expect(result.transactions[0].category).toBe('salary');
    expect(result.matchedRules['tx-1'].id).toBe('income-rule');
  });

  it('preserves manually categorized transactions', () => {
    const manual = transaction({ category: 'shopping', categorySource: 'manual' });

    const result = applyMerchantRules([manual], [rule()]);

    expect(result.transactions[0]).toEqual(manual);
    expect(result.matchedRules).toEqual({});
  });

  it('marks an automatically categorized transaction as rule-sourced without changing source metadata', () => {
    const imported = transaction({ source: 'Chase', categorySource: 'imported' });

    const result = applyMerchantRules([imported], [rule()]);

    expect(result.transactions[0]).toMatchObject({ category: 'food', categorySource: 'rule', source: 'Chase' });
  });

  it('treats legacy transactions without provenance as imported', () => {
    const legacy = transaction();

    const result = applyMerchantRules([legacy], [rule()]);

    expect(result.transactions[0].categorySource).toBe('rule');
  });

  it('normalizes rule alternatives the same way as merchants', () => {
    const result = applyMerchantRules([transaction({ merchant: 'SQ *Whole-Foods Market 1234 POS' })], [
      rule({ pattern: 'whole-foods market 999|other merchant' }),
    ]);

    expect(result.transactions[0].category).toBe('food');
  });

  it.each(['', '!!!', ' | #$% '])('does not let an empty rule pattern match every merchant', (pattern) => {
    const result = applyMerchantRules([transaction({ merchant: 'A completely different merchant' })], [rule({ pattern })]);

    expect(result.transactions[0].category).toBe('other');
    expect(result.matchedRules).toEqual({});
  });

  it('matches pattern text literally rather than interpreting punctuation as regex', () => {
    const result = applyMerchantRules([transaction({ merchant: 'A+B Market' })], [rule({ pattern: 'a+b' })]);

    expect(result.transactions[0].category).toBe('food');
  });

  it('records a transaction whose ID is __proto__ as an own matched-rule entry', () => {
    const matchedRule = rule();
    const result = applyMerchantRules([transaction({ id: '__proto__' })], [matchedRule]);

    expect(Object.prototype.hasOwnProperty.call(result.matchedRules, '__proto__')).toBe(true);
    expect(result.matchedRules['__proto__']).toBe(matchedRule);
  });

  it('applies rules through a dataset integration API without changing other collections', () => {
    const dataset = {
      accounts: [],
      transactions: [transaction()],
      assets: [],
      investmentHoldings: [],
      liabilities: [],
      recurringObligations: [],
      savingsGoals: [],
      merchantRules: [rule()],
    } satisfies FinancialDataset;

    const result = applyMerchantRulesToDataset(dataset);

    expect(result.transactions[0].category).toBe('food');
    expect(result.merchantRules).toBe(dataset.merchantRules);
    expect(result.accounts).toBe(dataset.accounts);
  });
});
