import { describe, expect, it } from 'vitest';
import {
  accountTotals,
  debtCents,
  monthlySpending,
  netWorthCents,
  savingsRate,
  spendingByCategory,
  sumCreditCardPayments,
  sumIncome,
  sumSpending,
  sumTransfers,
} from './calculations';
import type {
  Account,
  Asset,
  FinancialDataset,
  InvestmentHolding,
  Liability,
  Transaction,
} from './types';

const transactions: Transaction[] = [
  {
    id: 'groceries',
    date: '2026-01-05',
    merchant: 'Market',
    description: 'Groceries',
    amountCents: 1250,
    kind: 'spending',
    category: 'food',
    accountId: 'checking',
    source: 'manual',
    sourceFile: 'January.csv',
    reviewStatus: 'reviewed',
  },
  {
    id: 'rent',
    date: '2026-01-15',
    merchant: 'Landlord',
    description: 'Rent',
    amountCents: 200000,
    kind: 'spending',
    category: 'housing',
    accountId: 'checking',
    source: 'manual',
    sourceFile: 'January.csv',
    reviewStatus: 'reviewed',
  },
  {
    id: 'salary',
    date: '2026-01-20',
    merchant: 'Employer',
    description: 'Salary',
    amountCents: 500000,
    kind: 'income',
    category: 'salary',
    accountId: 'checking',
    source: 'manual',
    sourceFile: 'January.csv',
    reviewStatus: 'reviewed',
  },
  {
    id: 'transfer',
    date: '2026-01-21',
    merchant: 'Savings',
    description: 'Transfer to savings',
    amountCents: 100000,
    kind: 'transfer',
    category: 'transfer',
    accountId: 'checking',
    source: 'manual',
    sourceFile: 'January.csv',
    reviewStatus: 'reviewed',
  },
  {
    id: 'card-payment',
    date: '2026-02-02',
    merchant: 'Discover',
    description: 'Credit card payment',
    amountCents: 75000,
    kind: 'credit_card_payment',
    category: 'transfer',
    accountId: 'checking',
    source: 'manual',
    sourceFile: 'February.csv',
    reviewStatus: 'reviewed',
  },
  {
    id: 'dining',
    date: '2026-02-03',
    merchant: 'Cafe',
    description: 'Dinner',
    amountCents: 2500,
    kind: 'spending',
    category: 'food',
    accountId: 'card',
    source: 'manual',
    sourceFile: 'February.csv',
    reviewStatus: 'reviewed',
  },
];

const accounts: Account[] = [
  { id: 'checking', institution: 'Bank', name: 'Checking', kind: 'cash', balanceCents: 300000 },
  { id: 'card', institution: 'Discover', name: 'Card', kind: 'credit', balanceCents: -40000 },
  { id: 'brokerage', institution: 'Fidelity', name: 'Brokerage', kind: 'investment', balanceCents: 900000 },
];

const assets: Asset[] = [
  { id: 'home', name: 'Home', kind: 'property', valueCents: 25000000 },
  { id: 'car', name: 'Car', kind: 'vehicle', valueCents: 1500000 },
];

const holdings: InvestmentHolding[] = [
  { id: 'vti', accountId: 'brokerage', symbol: 'VTI', shares: 10, costBasisCents: 700000, marketValueCents: 900000 },
];

const liabilities: Liability[] = [
  { id: 'mortgage', name: 'Mortgage', kind: 'mortgage', balanceCents: 18000000 },
  { id: 'auto', name: 'Auto loan', kind: 'auto_loan', balanceCents: 500000 },
];

const dataset: FinancialDataset = {
  accounts,
  transactions,
  assets,
  investmentHoldings: holdings,
  liabilities,
  recurringObligations: [],
  savingsGoals: [],
  merchantRules: [],
};

const refund: Transaction = {
  id: 'refund',
  date: '2026-02-04',
  merchant: 'Cafe',
  description: 'Refund for dinner',
  amountCents: 500,
  kind: 'refund',
  category: 'food',
  accountId: 'card',
  source: 'manual',
  sourceFile: 'February.csv',
  reviewStatus: 'reviewed',
};

describe('financial calculations', () => {
  it('sums integer-cent spending without floating point drift', () => {
    expect(sumSpending(transactions)).toBe(203750);
  });

  it('excludes transfers and credit card payments from spending while retaining transfers', () => {
    expect(sumSpending(transactions)).toBe(203750);
    expect(sumTransfers(transactions)).toBe(100000);
    expect(sumCreditCardPayments(transactions)).toBe(75000);
  });

  it('separates income from spending', () => {
    expect(sumIncome(transactions)).toBe(500000);
    expect(sumSpending(transactions)).toBe(203750);
  });

  it('calculates savings rate from income and eligible spending', () => {
    expect(savingsRate(transactions)).toBeCloseTo(0.5925);
  });

  it('subtracts positive refunds from spending and savings calculations', () => {
    const transactionsWithRefund = [...transactions, refund];
    expect(sumSpending(transactionsWithRefund)).toBe(203250);
    expect(monthlySpending(transactionsWithRefund)).toEqual({ '2026-01': 201250, '2026-02': 2000 });
    expect(spendingByCategory(transactionsWithRefund)).toEqual({ food: 3250, housing: 200000 });
    expect(savingsRate([refund, { ...transactions[2], amountCents: 10000 }, { ...transactions[0], amountCents: 1000 }])).toBeCloseTo(0.95);
  });

  it('returns zero or empty totals for empty inputs', () => {
    expect(sumSpending([])).toBe(0);
    expect(sumIncome([])).toBe(0);
    expect(sumTransfers([])).toBe(0);
    expect(sumCreditCardPayments([])).toBe(0);
    expect(savingsRate([])).toBe(0);
    expect(monthlySpending([])).toEqual({});
    expect(spendingByCategory([])).toEqual({});
    expect(accountTotals([])).toEqual({});
  });

  it('calculates net worth from cash, investments, assets, and liabilities', () => {
    expect(netWorthCents(dataset)).toBe(9160000);
  });

  it('includes investment accounts without holdings alongside held investment accounts', () => {
    const mixedDataset = {
      ...dataset,
      accounts: [
        ...accounts,
        { id: 'robo', institution: 'Broker', name: 'Robo', kind: 'investment' as const, balanceCents: 100000 },
      ],
    };
    expect(netWorthCents(mixedDataset)).toBe(9260000);
  });

  it('groups spending by month in integer cents', () => {
    expect(monthlySpending(transactions)).toEqual({ '2026-01': 201250, '2026-02': 2500 });
  });

  it('groups spending by category', () => {
    expect(spendingByCategory(transactions)).toEqual({ food: 3750, housing: 200000 });
  });

  it('returns current totals by account kind', () => {
    expect(accountTotals(accounts)).toEqual({ cash: 300000, credit: -40000, investment: 900000 });
  });

  it('keeps credit balances signed and subtracts positive liability balances', () => {
    expect(accountTotals([{ ...accounts[1] }])).toEqual({ credit: -40000 });
    expect(netWorthCents({
      ...dataset,
      accounts: [{ ...accounts[0] }, { ...accounts[1] }],
      assets: [],
      investmentHoldings: [],
      liabilities: [{ id: 'loan', name: 'Loan', kind: 'other', balanceCents: 100000 }],
    })).toBe(160000);
  });

  it('uses debt accounts as the authority and only adds standalone liabilities', () => {
    expect(netWorthCents({
      ...dataset,
      accounts: [{ ...accounts[0] }, { ...accounts[1], balanceCents: -40000 }],
      assets: [],
      investmentHoldings: [],
      liabilities: [
        { id: 'card-liability', name: 'Card mirror', kind: 'credit_card', balanceCents: 40000 },
        { id: 'mortgage', name: 'Mortgage', kind: 'mortgage', balanceCents: 100000 },
      ],
    })).toBe(120000);
  });

  it('only suppresses a liability when its account identity meaningfully matches', () => {
    const value = {
      ...dataset,
      accounts: [
        { id: 'unrelated-credit', institution: 'Different Bank', name: 'Rewards Card', kind: 'credit' as const, balanceCents: -5000 },
      ],
      liabilities: [{ id: 'card-liability', name: 'Discover It Card', kind: 'credit_card' as const, balanceCents: 40000 }],
    };

    expect(debtCents(value)).toBe(45000);
  });
});
