import type { FinancialDataset, Transaction } from '../domain/types';

const accounts = [
  { id: 'discover-card', institution: 'Discover', name: 'It Cash Back', kind: 'credit' as const, balanceCents: -84215, lastImportedAt: '2026-08-01' },
  { id: 'chase-card', institution: 'Chase', name: 'Freedom Unlimited', kind: 'credit' as const, balanceCents: -126430, lastImportedAt: '2026-08-01' },
  { id: 'amex-card', institution: 'American Express', name: 'Blue Cash Preferred', kind: 'credit' as const, balanceCents: -63820, lastImportedAt: '2026-08-01' },
  { id: 'chase-checking', institution: 'Chase', name: 'Total Checking', kind: 'cash' as const, balanceCents: 842650, lastImportedAt: '2026-08-01' },
  { id: 'sofi-savings', institution: 'SoFi', name: 'Vaults and Savings', kind: 'cash' as const, balanceCents: 1245000, lastImportedAt: '2026-08-01' },
  { id: 'fidelity-brokerage', institution: 'Fidelity', name: 'Individual Brokerage', kind: 'investment' as const, balanceCents: 4832750, lastImportedAt: '2026-08-01' },
  { id: 'mazda-loan', institution: 'Mazda Bank', name: 'CX-5 Auto Loan', kind: 'loan' as const, balanceCents: -1824000, lastImportedAt: '2026-08-01' },
  { id: 'newrez-mortgage', institution: 'Newrez', name: 'Home Mortgage', kind: 'loan' as const, balanceCents: -28460000, lastImportedAt: '2026-08-01' },
];

const transaction = (
  id: string, date: string, merchant: string, amountCents: number,
  kind: Transaction['kind'], category: Transaction['category'], accountId: string,
): Transaction => ({
  id, date, merchant, description: merchant, amountCents, kind, category, accountId,
  source: accountId.includes('fidelity') ? 'Fidelity positions/activity' : accounts.find((account) => account.id === accountId)?.institution ?? 'Demo',
  categorySource: 'imported',
  sourceFile: `demo-${accountId}.csv`, reviewStatus: 'reviewed', sourceId: id,
});

const transactions: Transaction[] = [
  transaction('t-2026-08-pay', '2026-08-01', 'Acme Payroll', 615000, 'income', 'salary', 'chase-checking'),
  transaction('t-2026-07-pay', '2026-07-01', 'Acme Payroll', 615000, 'income', 'salary', 'chase-checking'),
  transaction('t-2026-06-pay', '2026-06-01', 'Acme Payroll', 615000, 'income', 'salary', 'chase-checking'),
  transaction('t-2026-08-mortgage', '2026-08-03', 'Newrez Mortgage', 218000, 'spending', 'housing', 'chase-checking'),
  transaction('t-2026-07-mortgage', '2026-07-03', 'Newrez Mortgage', 218000, 'spending', 'housing', 'chase-checking'),
  transaction('t-2026-06-mortgage', '2026-06-03', 'Newrez Mortgage', 218000, 'spending', 'housing', 'chase-checking'),
  transaction('t-2026-08-auto', '2026-08-10', 'Mazda Bank Payment', 48500, 'spending', 'transportation', 'mazda-loan'),
  transaction('t-2026-07-auto', '2026-07-10', 'Mazda Bank Payment', 48500, 'spending', 'transportation', 'mazda-loan'),
  transaction('t-2026-08-mortgage-loan', '2026-08-03', 'Newrez Principal Snapshot', 112000, 'transfer', 'transfer', 'newrez-mortgage'),
  transaction('t-2026-08-groceries', '2026-08-06', 'Whole Foods Market', 12842, 'spending', 'food', 'discover-card'),
  transaction('t-2026-07-groceries', '2026-07-06', 'Trader Joes', 9645, 'spending', 'food', 'chase-card'),
  transaction('t-2026-06-groceries', '2026-06-06', 'Kroger', 11280, 'spending', 'food', 'amex-card'),
  transaction('t-2026-08-fuel', '2026-08-09', 'Shell', 5840, 'spending', 'transportation', 'amex-card'),
  transaction('t-2026-07-fuel', '2026-07-09', 'Costco Gas', 6210, 'spending', 'transportation', 'discover-card'),
  transaction('t-2026-06-fuel', '2026-06-09', 'Chevron', 5985, 'spending', 'transportation', 'chase-card'),
  transaction('t-2026-08-internet', '2026-08-12', 'Fiber Internet', 7999, 'spending', 'utilities', 'chase-checking'),
  transaction('t-2026-07-internet', '2026-07-12', 'Fiber Internet', 7999, 'spending', 'utilities', 'chase-checking'),
  transaction('t-2026-06-internet', '2026-06-12', 'Fiber Internet', 7999, 'spending', 'utilities', 'chase-checking'),
  transaction('t-2026-08-transfer', '2026-08-15', 'SoFi Savings Transfer', 50000, 'transfer', 'transfer', 'chase-checking'),
  transaction('t-2026-07-transfer', '2026-07-15', 'SoFi Savings Transfer', 50000, 'transfer', 'transfer', 'chase-checking'),
  transaction('t-2026-08-card-payment', '2026-08-20', 'Chase Credit Card Payment', 90000, 'credit_card_payment', 'transfer', 'chase-checking'),
  transaction('t-2026-07-dividend', '2026-07-29', 'Fidelity Dividend', 3820, 'income', 'other', 'fidelity-brokerage'),
  transaction('t-2026-06-dividend', '2026-06-29', 'Fidelity Dividend', 3760, 'income', 'other', 'fidelity-brokerage'),
  transaction('t-2026-07-invest', '2026-07-10', 'Fidelity VTI Purchase', 25000, 'investment', 'transfer', 'fidelity-brokerage'),
  transaction('t-2026-06-refund', '2026-06-18', 'Outdoor Supply Refund', 4500, 'refund', 'shopping', 'discover-card'),
];

const demoDatasetTemplate: FinancialDataset = {
  accounts,
  transactions,
  assets: [
    { id: 'home', name: 'Primary residence', kind: 'property', valueCents: 41200000 },
    { id: 'mazda-cx5', name: '2022 Mazda CX-5', kind: 'vehicle', valueCents: 2140000 },
  ],
  investmentHoldings: [
    { id: 'holding-vti', accountId: 'fidelity-brokerage', symbol: 'VTI', shares: 18.42, costBasisCents: 3521000, marketValueCents: 4052400 },
    { id: 'holding-bnd', accountId: 'fidelity-brokerage', symbol: 'BND', shares: 32.1, costBasisCents: 235000, marketValueCents: 241350 },
    { id: 'holding-msft', accountId: 'fidelity-brokerage', symbol: 'MSFT', shares: 2.5, costBasisCents: 89000, marketValueCents: 139000 },
  ],
  liabilities: [
    { id: 'mortgage', name: 'Newrez mortgage', kind: 'mortgage', balanceCents: 28460000, interestRate: 0.0375, minimumPaymentCents: 218000 },
    { id: 'auto-loan', name: 'Mazda Bank auto loan', kind: 'auto_loan', balanceCents: 1824000, interestRate: 0.0499, minimumPaymentCents: 48500 },
  ],
  recurringObligations: [
    { id: 'ob-mortgage', name: 'Mortgage', category: 'housing', amountCents: 218000, frequency: 'monthly', nextDueDate: '2026-09-03' },
    { id: 'ob-auto', name: 'Mazda payment', category: 'transportation', amountCents: 48500, frequency: 'monthly', nextDueDate: '2026-09-10' },
    { id: 'ob-internet', name: 'Fiber Internet', category: 'utilities', amountCents: 7999, frequency: 'monthly', nextDueDate: '2026-09-12' },
    { id: 'ob-cloud', name: 'Cloud storage', category: 'subscriptions', amountCents: 999, frequency: 'monthly', nextDueDate: '2026-09-18' },
  ],
  savingsGoals: [{ id: 'goal-emergency', name: 'Emergency fund', targetCents: 1800000, currentCents: 1245000, deadline: '2027-06-30' }],
  merchantRules: [
    { id: 'rule-grocery', pattern: 'whole foods|trader joes|kroger', category: 'food' },
    { id: 'rule-fuel', pattern: 'shell|costco gas|chevron', category: 'transportation' },
  ],
  netWorthSnapshots: [
    { date: '2026-06-30', assetsCents: 51430000, liabilitiesCents: 30410000, netWorthCents: 21020000 },
    { date: '2026-07-31', assetsCents: 52010000, liabilitiesCents: 30320000, netWorthCents: 21690000 },
    { date: '2026-08-21', assetsCents: 52832750, liabilitiesCents: 30284000, netWorthCents: 22548750 },
  ],
};

export function createDemoDataset(): FinancialDataset {
  return structuredClone(demoDatasetTemplate);
}
