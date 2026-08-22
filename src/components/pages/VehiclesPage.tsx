import type { FinancialDataset } from '../../domain/types';
import SectionHeader from '../ui/SectionHeader';
import type { DateRange } from '../dashboard/view-utils';
import { PageFrame, RangeNote, money } from './page-utils';
import { filterSpendingTransactions } from '../dashboard/view-utils';
import EmptyState from '../ui/EmptyState';
import type { RouteId } from '../layout/AppShell';

export default function VehiclesPage({ dataset, dateRange, privacyVisible, onNavigate = () => undefined }: { dataset: FinancialDataset; dateRange: DateRange; privacyVisible: boolean; onNavigate?: (route: RouteId) => void }) {
  const vehicles = dataset.assets.filter((asset) => asset.kind === 'vehicle');
  const costs = filterSpendingTransactions(dataset.transactions, dateRange).filter((transaction) => transaction.category === 'transportation');
  const autoLoans = dataset.liabilities.filter((liability) => liability.kind === 'auto_loan');
  return <PageFrame title="Vehicles" eyebrow="Mobility" description="Vehicle value, loan balance, and transport costs."><RangeNote dateRange={dateRange} /><SectionHeader title="Vehicles" eyebrow="Assets" />{vehicles.length === 0 ? <EmptyState title="No vehicles recorded" description="Add a vehicle to track its value and related costs." action={<button className="button" type="button" onClick={() => onNavigate('vehicles')}>Add vehicle</button>} /> : <div className="data-list">{vehicles.map((asset) => <div className="data-row" key={asset.id}><span>{asset.name}</span><strong>{money(asset.valueCents, privacyVisible)}</strong></div>)}</div>}<SectionHeader title="Auto loan balance" eyebrow="Liability" />{autoLoans.length > 0 && <div className="data-list">{autoLoans.map((liability) => <div className="data-row" key={liability.id}><span>{liability.name}</span><strong>{money(liability.balanceCents, privacyVisible)}</strong></div>)}</div>}<SectionHeader title="Vehicle costs" eyebrow="Transactions" />{costs.length === 0 ? <EmptyState title="No vehicle costs" description="Import transportation transactions to see costs for this range." action={<button className="button" type="button" onClick={() => onNavigate('import')}>Import transactions</button>} /> : <div className="data-list">{costs.map((transaction) => <div className="data-row" key={transaction.id}><span>{transaction.merchant}<small>{transaction.date}</small></span><strong>{money(transaction.kind === 'refund' ? -transaction.amountCents : transaction.amountCents, privacyVisible)}</strong></div>)}</div>}</PageFrame>;
}
