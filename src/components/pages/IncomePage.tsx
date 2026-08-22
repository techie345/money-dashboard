import { sumIncome } from '../../domain/calculations';
import type { FinancialDataset } from '../../domain/types';
import SectionHeader from '../ui/SectionHeader';
import { filterTransactions, type DateRange } from '../dashboard/view-utils';
import { Label, PageFrame, RangeNote, money } from './page-utils';
import EmptyState from '../ui/EmptyState';
import type { RouteId } from '../layout/AppShell';

export default function IncomePage({ dataset, dateRange, privacyVisible, onNavigate = () => undefined }: { dataset: FinancialDataset; dateRange: DateRange; privacyVisible: boolean; onNavigate?: (route: RouteId) => void }) {
  const income = filterTransactions(dataset.transactions, dateRange).filter((transaction) => transaction.kind === 'income');
  return <PageFrame title="Income" eyebrow="Inflows" description="Salary, other income, recurring commitments, and liabilities."><RangeNote dateRange={dateRange} /><SectionHeader title="Income history" eyebrow="Deposits" />{income.length === 0 ? <EmptyState title="No income data" description="Import income transactions to see deposits for this range." action={<button className="button" type="button" onClick={() => onNavigate('import')}>Import transactions</button>} /> : <div className="data-list">{income.map((transaction) => <div className="data-row" key={transaction.id}><span>{transaction.merchant}<small><Label value={transaction.category} /> · {transaction.date}</small></span><strong>{money(transaction.amountCents, privacyVisible)}</strong></div>)}</div>}<p className="page-total">Selected income <strong>{money(sumIncome(income), privacyVisible)}</strong></p><SectionHeader title="Liabilities" eyebrow="Debt" />{dataset.liabilities.length === 0 ? <EmptyState title="No liabilities recorded" description="Add a liability or import a debt account." action={<button className="button" type="button" onClick={() => onNavigate('income')}>Add liability</button>} /> : <div className="data-list">{dataset.liabilities.map((liability) => <div className="data-row" key={liability.id}><span>{liability.name}<small>{liability.interestRate ? `${(liability.interestRate * 100).toFixed(2)}% interest` : ''}</small></span><strong>{money(liability.balanceCents, privacyVisible)}</strong></div>)}</div>}</PageFrame>;
}
