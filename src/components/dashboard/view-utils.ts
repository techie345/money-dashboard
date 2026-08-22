import type { Account, FinancialDataset, NetWorthSnapshot, RecurringObligation, Transaction } from '../../domain/types';

export type DateRange = '3' | '6' | '12' | 'all';

export const formatMoney = (cents: number) => new Intl.NumberFormat('en-US', {
  style: 'currency', currency: 'USD', maximumFractionDigits: 0,
}).format(cents / 100);

export const titleCase = (value: string) => value.replaceAll('_', ' ').replace(/\b\w/g, (letter) => letter.toUpperCase());

const cutoffFor = (dates: string[], range: DateRange) => {
  if (range === 'all' || dates.length === 0) return undefined;
  const latestMonth = dates.map((date) => date.slice(0, 7)).sort().at(-1)!;
  const [year, month] = latestMonth.split('-').map(Number);
  const cutoff = new Date(Date.UTC(year, month - Number(range), 1));
  return cutoff.toISOString().slice(0, 7);
};

export const filterTransactions = (transactions: Transaction[], range: DateRange) => {
  const cutoff = cutoffFor(transactions.map((transaction) => transaction.date), range);
  return cutoff ? transactions.filter((transaction) => transaction.date.slice(0, 7) >= cutoff) : transactions;
};

export const filterSnapshots = (snapshots: NetWorthSnapshot[], range: DateRange) => {
  const cutoff = cutoffFor(snapshots.map((snapshot) => snapshot.date), range);
  return [...snapshots].filter((snapshot) => !cutoff || snapshot.date.slice(0, 7) >= cutoff)
    .sort((left, right) => left.date.localeCompare(right.date));
};

export const filterSpendingTransactions = (transactions: Transaction[], range: DateRange) =>
  filterTransactions(transactions, range).filter((transaction) => transaction.kind === 'spending' || transaction.kind === 'refund');

export const upcomingObligations = (obligations: RecurringObligation[], asOf: string) => obligations
  .filter((obligation) => obligation.nextDueDate && obligation.nextDueDate >= asOf)
  .sort((left, right) => (left.nextDueDate ?? '').localeCompare(right.nextDueDate ?? ''));

export const cashAccounts = (accounts: Account[]) => accounts.filter((account) => account.kind === 'cash');

export const latestUpdatedDate = (dataset: FinancialDataset) => {
  const dates = [
    ...dataset.accounts.map((account) => account.lastImportedAt),
    ...dataset.transactions.map((transaction) => transaction.importedAt),
    ...(dataset.netWorthSnapshots ?? []).map((snapshot) => snapshot.date),
  ].filter(Boolean) as string[];
  return dates.reduce((latest, date) => date > latest ? date : latest, '') || 'Not yet';
};
