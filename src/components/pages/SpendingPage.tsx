import { spendingByCategory, sumSpending } from '../../domain/calculations';
import type { FinancialDataset } from '../../domain/types';
import SectionHeader from '../ui/SectionHeader';
import { filterTransactions, type DateRange } from '../dashboard/view-utils';
import { PageFrame, RangeNote, money } from './page-utils';
import EmptyState from '../ui/EmptyState';
import type { RouteId } from '../layout/AppShell';

export default function SpendingPage({ dataset, dateRange, privacyVisible, onNavigate = () => undefined }: { dataset: FinancialDataset; dateRange: DateRange; privacyVisible: boolean; onNavigate?: (route: RouteId) => void }) {
  const transactions = filterTransactions(dataset.transactions, dateRange);
  const categories = Object.entries(spendingByCategory(transactions)).sort(([, left], [, right]) => right - left);
  return <PageFrame title="Spending" eyebrow="Outflows" description="See where money is going and which obligations repeat."><RangeNote dateRange={dateRange} /><SectionHeader title="By category" eyebrow="Category signal" />{categories.length === 0 ? <EmptyState title="No spending data" description="Import transactions to see spending by category." action={<button className="button" type="button" onClick={() => onNavigate('import')}>Import transactions</button>} /> : <div className="data-list">{categories.map(([category, amount]) => <div className="data-row" key={category}><span>{category.replaceAll('_', ' ').replace(/\b\w/g, (letter) => letter.toUpperCase())}</span><strong>{money(amount, privacyVisible)}</strong></div>)}</div>}<SectionHeader title="Recurring obligations" eyebrow="Committed" />{dataset.recurringObligations.length === 0 ? <EmptyState title="No recurring obligations" description="Add a recurring obligation to track committed spending." action={<button className="button" type="button" onClick={() => onNavigate('spending')}>Add obligation</button>} /> : <div className="data-list">{dataset.recurringObligations.map((obligation) => <div className="data-row" key={obligation.id}><span>{obligation.name}<small>{obligation.frequency}</small></span><strong>{money(obligation.amountCents, privacyVisible)}</strong></div>)}</div>}<p className="page-total">Selected spending <strong>{money(sumSpending(transactions), privacyVisible)}</strong></p></PageFrame>;
}
