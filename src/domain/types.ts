import type { Category } from './categories';

export type AccountKind = 'cash' | 'credit' | 'investment' | 'loan' | 'other';
export type TransactionKind =
  | 'spending'
  | 'income'
  | 'transfer'
  | 'credit_card_payment'
  | 'investment'
  | 'refund';
export type ReviewStatus = 'reviewed' | 'needs_review';
export type TransactionDirection = 'inflow' | 'outflow';

export interface Account {
  id: string;
  institution: string;
  name: string;
  kind: AccountKind;
  /** Asset-like balances are positive; debt account balances (credit/loan) are negative. */
  balanceCents: number;
  lastImportedAt?: string;
  source?: string;
  sourceFile?: string;
  importedAt?: string;
}

export interface Transaction {
  id: string;
  date: string;
  merchant: string;
  description: string;
  /** Always a positive magnitude; kind determines whether it is income, spending, or a refund. */
  amountCents: number;
  kind: TransactionKind;
  category: Category;
  accountId: string;
  source: string;
  sourceFile: string;
  reviewStatus: ReviewStatus;
  direction?: TransactionDirection;
  categorySource?: 'imported' | 'manual' | 'rule';
  sourceId?: string;
  transferType?: 'account_transfer' | 'credit_card_payment';
  importedAt?: string;
  action?: string;
  symbol?: string;
  shares?: number;
  priceCents?: number;
}

export interface Asset {
  id: string;
  name: string;
  kind: 'property' | 'vehicle' | 'other';
  valueCents: number;
}

export interface InvestmentHolding {
  id: string;
  accountId: string;
  symbol: string;
  shares: number;
  costBasisCents: number;
  marketValueCents: number;
  source?: string;
  sourceFile?: string;
  importedAt?: string;
}

export interface Liability {
  id: string;
  name: string;
  kind: 'mortgage' | 'auto_loan' | 'student_loan' | 'credit_card' | 'other';
  /** Outstanding debt is stored as a positive amount and is subtracted from net worth. */
  balanceCents: number;
  interestRate?: number;
  minimumPaymentCents?: number;
  accountId?: string;
  source?: string;
  sourceFile?: string;
  importedAt?: string;
}

export interface RecurringObligation {
  id: string;
  name: string;
  category: Category;
  amountCents: number;
  frequency: 'weekly' | 'monthly' | 'yearly';
  nextDueDate?: string;
}

export interface SavingsGoal {
  id: string;
  name: string;
  targetCents: number;
  currentCents: number;
  deadline?: string;
}

export interface NetWorthSnapshot {
  date: string;
  assetsCents: number;
  liabilitiesCents: number;
  netWorthCents: number;
  accountId?: string;
  source?: string;
  sourceFile?: string;
  importedAt?: string;
}

export interface MerchantRule {
  id: string;
  pattern: string;
  category: Category;
  accountId?: string;
  kind?: TransactionKind;
}

export interface FinancialDataset {
  accounts: Account[];
  transactions: Transaction[];
  assets: Asset[];
  investmentHoldings: InvestmentHolding[];
  liabilities: Liability[];
  recurringObligations: RecurringObligation[];
  savingsGoals: SavingsGoal[];
  merchantRules: MerchantRule[];
  netWorthSnapshots?: NetWorthSnapshot[];
}
