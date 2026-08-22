import type { FinancialDataset } from '../../domain/types';
import SectionHeader from '../ui/SectionHeader';
import type { DateRange } from '../dashboard/view-utils';
import { PageFrame, RangeNote, money } from './page-utils';
import { filterSpendingTransactions } from '../dashboard/view-utils';
import EmptyState from '../ui/EmptyState';
import type { RouteId } from '../layout/AppShell';

export default function PropertyPage({ dataset, dateRange, privacyVisible, onNavigate = () => undefined }: { dataset: FinancialDataset; dateRange: DateRange; privacyVisible: boolean; onNavigate?: (route: RouteId) => void }) {
  const property = dataset.assets.filter((asset) => asset.kind === 'property');
  const housing = filterSpendingTransactions(dataset.transactions, dateRange).filter((transaction) => transaction.category === 'housing');
  const mortgage = dataset.liabilities.filter((liability) => liability.kind === 'mortgage');
  return <PageFrame title="Property" eyebrow="Home base" description="Housing value, mortgage liability, and ongoing costs."><RangeNote dateRange={dateRange} /><SectionHeader title="Property value" eyebrow="Assets" />{property.length === 0 ? <EmptyState title="No property recorded" description="Add a property to include its value in net worth." action={<button className="button" type="button" onClick={() => onNavigate('property')}>Add property</button>} /> : <div className="data-list">{property.map((asset) => <div className="data-row" key={asset.id}><span>{asset.name}</span><strong>{money(asset.valueCents, privacyVisible)}</strong></div>)}</div>}<SectionHeader title="Mortgage balance" eyebrow="Liability" />{mortgage.length > 0 && <div className="data-list">{mortgage.map((liability) => <div className="data-row" key={liability.id}><span>{liability.name}</span><strong>{money(liability.balanceCents, privacyVisible)}</strong></div>)}</div>}<SectionHeader title="Housing costs" eyebrow="Transactions" />{housing.length === 0 ? <EmptyState title="No housing costs" description="Import housing transactions to see costs for this range." action={<button className="button" type="button" onClick={() => onNavigate('import')}>Import transactions</button>} /> : <div className="data-list">{housing.map((transaction) => <div className="data-row" key={transaction.id}><span>{transaction.merchant}<small>{transaction.date}</small></span><strong>{money(transaction.kind === 'refund' ? -transaction.amountCents : transaction.amountCents, privacyVisible)}</strong></div>)}</div>}</PageFrame>;
}
