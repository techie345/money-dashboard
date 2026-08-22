import { createDemoDataset } from './demo-data';
import { loadDataset, saveDataset } from './storage';
import { CATEGORIES } from '../domain/categories';
import type { FinancialDataset } from '../domain/types';

export interface DatasetStorage {
  loadDataset: () => Promise<FinancialDataset | undefined>;
  saveDataset: (dataset: FinancialDataset) => Promise<void>;
}

const accountKinds = new Set(['cash', 'credit', 'investment', 'loan', 'other']);
const transactionKinds = new Set(['spending', 'income', 'transfer', 'credit_card_payment', 'investment', 'refund']);
const reviewStatuses = new Set(['reviewed', 'needs_review']);
const assetKinds = new Set(['property', 'vehicle', 'other']);
const liabilityKinds = new Set(['mortgage', 'auto_loan', 'student_loan', 'credit_card', 'other']);
const frequencies = new Set(['weekly', 'monthly', 'yearly']);
const categorySources = new Set(['imported', 'manual', 'rule']);

const isRecord = (value: unknown): value is Record<string, unknown> => typeof value === 'object' && value !== null;
const isString = (value: unknown): value is string => typeof value === 'string' && value.trim().length > 0;
const isNumber = (value: unknown): value is number => typeof value === 'number' && Number.isFinite(value);
const isNonNegativeNumber = (value: unknown): value is number => isNumber(value) && value >= 0;
const isPositiveNumber = (value: unknown): value is number => isNumber(value) && value > 0;
const hasIdentity = (value: unknown): value is Record<string, unknown> => isRecord(value) && isString(value.id) && isString(value.name);
const hasCategory = (value: unknown): boolean => isString(value) && CATEGORIES.includes(value as (typeof CATEGORIES)[number]);
const isDate = (value: unknown): value is string => {
  if (!isString(value) || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
  const parsed = new Date(`${value}T00:00:00Z`);
  return !Number.isNaN(parsed.getTime())
    && parsed.toISOString().slice(0, 10) === value;
};
const hasUniqueIds = (values: unknown[]): boolean => {
  const ids = values.map((value) => isRecord(value) ? value.id : undefined);
  return ids.every(isString) && new Set(ids).size === ids.length;
};

function isValidDataset(value: unknown): value is FinancialDataset {
  if (!isRecord(value)) return false;
  const accounts = value.accounts;
  const transactions = value.transactions;
  const assets = value.assets;
  const holdings = value.investmentHoldings;
  const liabilities = value.liabilities;
  const obligations = value.recurringObligations;
  const goals = value.savingsGoals;
  const rules = value.merchantRules;
  const snapshots = value.netWorthSnapshots;

  const accountIds = new Set((Array.isArray(accounts) ? accounts : []).filter(isRecord).map((account) => account.id));

  return Array.isArray(accounts) && hasUniqueIds(accounts) && accounts.every((account) => isRecord(account)
    && isString(account.id) && isString(account.institution) && isString(account.name)
    && accountKinds.has(account.kind as string) && isNumber(account.balanceCents)
    && (account.lastImportedAt === undefined || isDate(account.lastImportedAt)))
    && Array.isArray(transactions) && hasUniqueIds(transactions) && transactions.every((transaction) => isRecord(transaction)
      && isString(transaction.id) && isDate(transaction.date) && isString(transaction.merchant)
       && isString(transaction.description) && isPositiveNumber(transaction.amountCents)
       && transactionKinds.has(transaction.kind as string) && hasCategory(transaction.category)
       && isString(transaction.accountId) && accountIds.has(transaction.accountId)
        && isString(transaction.source) && isString(transaction.sourceFile)
        && reviewStatuses.has(transaction.reviewStatus as string)
        && (transaction.categorySource === undefined || categorySources.has(transaction.categorySource as string))
        && (transaction.sourceId === undefined || isString(transaction.sourceId)))
    && Array.isArray(assets) && hasUniqueIds(assets) && assets.every((asset) => hasIdentity(asset)
      && assetKinds.has(asset.kind as string) && isNumber(asset.valueCents))
    && Array.isArray(holdings) && hasUniqueIds(holdings) && holdings.every((holding) => isRecord(holding)
      && isString(holding.id) && isString(holding.accountId) && isString(holding.symbol)
      && accountIds.has(holding.accountId)
      && isNumber(holding.shares) && isNumber(holding.costBasisCents) && isNumber(holding.marketValueCents))
    && Array.isArray(liabilities) && hasUniqueIds(liabilities) && liabilities.every((liability) => hasIdentity(liability)
       && liabilityKinds.has(liability.kind as string) && isPositiveNumber(liability.balanceCents)
       && (liability.interestRate === undefined || isNonNegativeNumber(liability.interestRate))
       && (liability.minimumPaymentCents === undefined || isNonNegativeNumber(liability.minimumPaymentCents)))
    && Array.isArray(obligations) && hasUniqueIds(obligations) && obligations.every((obligation) => isRecord(obligation)
      && isString(obligation.id) && isString(obligation.name) && hasCategory(obligation.category)
      && isNumber(obligation.amountCents) && frequencies.has(obligation.frequency as string)
      && (obligation.nextDueDate === undefined || isDate(obligation.nextDueDate)))
    && Array.isArray(goals) && hasUniqueIds(goals) && goals.every((goal) => hasIdentity(goal)
      && isNumber(goal.targetCents) && isNumber(goal.currentCents)
      && (goal.deadline === undefined || isDate(goal.deadline)))
     && Array.isArray(rules) && hasUniqueIds(rules) && rules.every((rule) => isRecord(rule)
       && isString(rule.id) && isString(rule.pattern) && hasCategory(rule.category)
       && (rule.accountId === undefined || isString(rule.accountId) && accountIds.has(rule.accountId))
       && (rule.kind === undefined || transactionKinds.has(rule.kind as string)))
    && (snapshots === undefined || Array.isArray(snapshots) && snapshots.every((snapshot) => isRecord(snapshot)
      && isDate(snapshot.date) && isNumber(snapshot.assetsCents)
      && isNumber(snapshot.liabilitiesCents) && isNumber(snapshot.netWorthCents)));
}

export async function loadOrSeedDataset(storage: DatasetStorage = { loadDataset, saveDataset }): Promise<FinancialDataset> {
  const persisted = await storage.loadDataset();
  if (isValidDataset(persisted)) return persisted;
  const demoDataset = createDemoDataset();
  await storage.saveDataset(demoDataset);
  return demoDataset;
}
