import type { FinancialDataset, MerchantRule, Transaction } from './types';

const paymentPrefixes = /^(?:sq|tst|paypal)\s+/;
const paymentSuffixes = /\s+(?:pos|purchase|debit|credit|payment|pmt|inc|llc|ltd)$/;

export function normalizeMerchantName(merchant: string): string {
  let normalized = merchant
    .toLowerCase()
    .replace(/[\u0000-\u002f\u003a-\u0040\u005b-\u0060\u007b-\u007f]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();

  normalized = normalized.replace(paymentPrefixes, '').replace(/\s+#?\d+$/, '');
  while (paymentSuffixes.test(normalized)) normalized = normalized.replace(paymentSuffixes, '');
  return normalized.trim();
}

function normalizeRulePatterns(pattern: string): string[] {
  return pattern.split('|')
    .map(normalizeMerchantName)
    .filter((alternative) => alternative.length > 0);
}

function matchesRule(transaction: Transaction, rule: MerchantRule): boolean {
  if (rule.accountId !== undefined && rule.accountId !== transaction.accountId) return false;
  if (rule.kind !== undefined && rule.kind !== transaction.kind) return false;

  const merchant = normalizeMerchantName(transaction.merchant);
  return normalizeRulePatterns(rule.pattern).some((pattern) => merchant.includes(pattern));
}

export interface MerchantRuleResult {
  transactions: Transaction[];
  matchedRules: Record<string, MerchantRule>;
}

export function applyMerchantRules(transactions: Transaction[], rules: MerchantRule[]): MerchantRuleResult {
  const matchedRules: Record<string, MerchantRule> = Object.create(null);
  const updatedTransactions = transactions.map((transaction) => {
    if (transaction.categorySource === 'manual') return transaction;

    const matchedRule = rules.find((rule) => matchesRule(transaction, rule));
    if (matchedRule === undefined) return transaction;

    matchedRules[transaction.id] = matchedRule;
    return { ...transaction, category: matchedRule.category, categorySource: 'rule' as const };
  });

  return { transactions: updatedTransactions, matchedRules };
}

export function applyMerchantRulesToDataset(
  dataset: FinancialDataset,
  rules: MerchantRule[] = dataset.merchantRules,
): FinancialDataset {
  return { ...dataset, transactions: applyMerchantRules(dataset.transactions, rules).transactions };
}
