import type { Account, FinancialDataset, Liability, Transaction } from './types';

const isSpending = (transaction: Transaction) =>
  transaction.kind === 'spending' || transaction.kind === 'refund';

const spendingAmount = (transaction: Transaction) =>
  transaction.kind === 'refund' ? -transaction.amountCents : transaction.amountCents;

export function sumSpending(transactions: Transaction[]): number {
  return transactions.reduce(
    (total, transaction) => total + (isSpending(transaction) ? spendingAmount(transaction) : 0),
    0,
  );
}

export function sumIncome(transactions: Transaction[]): number {
  return transactions.reduce(
    (total, transaction) => total + (transaction.kind === 'income' ? transaction.amountCents : 0),
    0,
  );
}

/** Sums account-to-account transfers, excluding credit-card payment rows. */
export function sumTransfers(transactions: Transaction[]): number {
  return transactions.reduce(
    (total, transaction) =>
      total + (transaction.kind === 'transfer' ? transaction.amountCents : 0),
    0,
  );
}

export function sumCreditCardPayments(transactions: Transaction[]): number {
  return transactions.reduce(
    (total, transaction) =>
      total + (transaction.kind === 'credit_card_payment' ? transaction.amountCents : 0),
    0,
  );
}

export function savingsRate(transactions: Transaction[]): number {
  const income = sumIncome(transactions);
  return income === 0 ? 0 : (income - sumSpending(transactions)) / income;
}

export function netWorthCents(dataset: FinancialDataset): number {
  const cash = dataset.accounts
    .filter((account) => account.kind === 'cash')
    .reduce((total, account) => total + account.balanceCents, 0);
  const holdingAccountIds = new Set(dataset.investmentHoldings.map((holding) => holding.accountId));
  const investments = dataset.investmentHoldings.reduce(
    (total, holding) => total + holding.marketValueCents,
    0,
  ) + dataset.accounts
    .filter((account) => account.kind === 'investment' && !holdingAccountIds.has(account.id))
    .reduce((total, account) => total + account.balanceCents, 0);
  const debt = debtCents(dataset);
  const assets = dataset.assets.reduce((total, asset) => total + asset.valueCents, 0);

  return cash + investments + assets - debt;
}

const liabilityMatchesAccount = (liability: Liability, account: Account) => {
  if (liability.kind === 'credit_card' && account.kind !== 'credit') return false;
  if (liability.kind !== 'credit_card' && account.kind !== 'loan') return false;
  const ignoredWords = new Set(['account', 'card', 'cash', 'back', 'bank', 'home', 'loan', 'the', 'and', 'for', 'it']);
  const words = (value: string) => value.toLowerCase().split(/\W+/).filter((word) => word.length > 2 && !ignoredWords.has(word));
  const liabilityWords = words(liability.name);
  const accountWords = new Set([...words(account.institution), ...words(account.name)]);
  return liabilityWords.some((word) => accountWords.has(word));
};

/** Debt accounts are authoritative; liabilities only fill gaps without a matching debt account. */
export function debtCents(dataset: FinancialDataset): number {
  const debtAccounts = dataset.accounts.filter((account) => account.kind === 'credit' || account.kind === 'loan');
  return debtAccounts.reduce((total, account) => total + Math.abs(account.balanceCents), 0)
    + dataset.liabilities
      .filter((liability) => !debtAccounts.some((account) => liabilityMatchesAccount(liability, account)))
      .reduce((total, liability) => total + liability.balanceCents, 0);
}

export function investmentValueCents(dataset: FinancialDataset): number {
  const holdingAccountIds = new Set(dataset.investmentHoldings.map((holding) => holding.accountId));
  return dataset.investmentHoldings.reduce((total, holding) => total + holding.marketValueCents, 0)
    + dataset.accounts
      .filter((account) => account.kind === 'investment' && !holdingAccountIds.has(account.id))
      .reduce((total, account) => total + account.balanceCents, 0);
}

export function monthlySpending(transactions: Transaction[]): Record<string, number> {
  return transactions.reduce<Record<string, number>>((months, transaction) => {
    if (!isSpending(transaction)) return months;
    const month = transaction.date.slice(0, 7);
    months[month] = (months[month] ?? 0) + spendingAmount(transaction);
    return months;
  }, {});
}

export function spendingByCategory(transactions: Transaction[]): Record<string, number> {
  return transactions.reduce<Record<string, number>>((categories, transaction) => {
    if (!isSpending(transaction)) return categories;
    categories[transaction.category] =
      (categories[transaction.category] ?? 0) + spendingAmount(transaction);
    return categories;
  }, {});
}

export function accountTotals(accounts: Account[]): Record<string, number> {
  return accounts.reduce<Record<string, number>>((totals, account) => {
    totals[account.kind] = (totals[account.kind] ?? 0) + account.balanceCents;
    return totals;
  }, {});
}
