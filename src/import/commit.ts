import type { FinancialDataset, InvestmentHolding, Liability, NetWorthSnapshot } from '../domain/types';
import type { NormalizeResult } from './normalize';
import { upsertHoldings } from './dedupe';

export function createImportedDataset(dataset: FinancialDataset, normalized: NormalizeResult, incomingTransactions: NormalizeResult['transactions']): FinancialDataset {
  const balanceByAccount = new Map(normalized.balances.map((balance) => [balance.accountId, balance]));
  const importMetadata = normalized.balances[0] ?? normalized.positions[0] ?? normalized.transactions[0];
  const accounts = dataset.accounts.map((account) => {
    const balance = balanceByAccount.get(account.id);
    const metadata = balance?.accountId === account.id || (!balance && normalized.transactions.some((item) => item.accountId === account.id) || normalized.positions.some((item) => item.accountId === account.id)) ? balance ?? importMetadata : undefined;
    return metadata ? { ...account, ...(balance ? { balanceCents: -balance.balanceCents } : {}), lastImportedAt: metadata.importedAt?.slice(0, 10), source: metadata.source, sourceFile: metadata.sourceFile, importedAt: metadata.importedAt } : account;
  });
  const liabilities = [...dataset.liabilities];
  const snapshots = [...(dataset.netWorthSnapshots ?? [])];
  for (const balance of normalized.balances) {
    const account = accounts.find((item) => item.id === balance.accountId);
    const liability: Liability = { id: `${balance.accountId}-${balance.kind}`, accountId: balance.accountId, name: account?.name ?? balance.kind, kind: balance.kind, balanceCents: balance.balanceCents, source: balance.source, sourceFile: balance.sourceFile, importedAt: balance.importedAt };
    const index = liabilities.findIndex((item) => item.accountId === balance.accountId && item.kind === balance.kind);
    if (index === -1) liabilities.push(liability);
    else liabilities[index] = { ...liabilities[index], ...liability, id: liabilities[index].id };
    const assetsCents = accounts.filter((item) => item.balanceCents > 0).reduce((sum, item) => sum + item.balanceCents, 0)
      + dataset.assets.reduce((sum, item) => sum + item.valueCents, 0)
      + dataset.investmentHoldings.reduce((sum, item) => sum + item.marketValueCents, 0);
     const liabilitiesCents = liabilities.reduce((sum, item) => sum + (item.accountId === balance.accountId && item.kind === balance.kind ? balance.balanceCents : item.balanceCents), 0);
    const snapshot: NetWorthSnapshot = { date: balance.date, accountId: balance.accountId, assetsCents, liabilitiesCents, netWorthCents: assetsCents - liabilitiesCents, source: balance.source, sourceFile: balance.sourceFile, importedAt: balance.importedAt };
    const snapshotIndex = snapshots.findIndex((item) => item.date === snapshot.date && item.accountId === snapshot.accountId);
    if (snapshotIndex === -1) snapshots.push(snapshot);
    else snapshots[snapshotIndex] = snapshot;
  }
  const holdings: InvestmentHolding[] = normalized.positions.map((position) => ({ ...position, costBasisCents: position.costBasisCents ?? 0 }));
  return { ...dataset, accounts, liabilities, netWorthSnapshots: snapshots, transactions: [...dataset.transactions, ...incomingTransactions], investmentHoldings: upsertHoldings(dataset.investmentHoldings, holdings) };
}
