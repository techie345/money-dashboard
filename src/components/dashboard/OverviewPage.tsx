import { debtCents, investmentValueCents, netWorthCents, savingsRate, sumIncome, sumSpending } from '../../domain/calculations';
import type { FinancialDataset } from '../../domain/types';
import MetricCard from '../ui/MetricCard';
import PrivacyValue from '../ui/PrivacyValue';
import SectionHeader from '../ui/SectionHeader';
import EmptyState from '../ui/EmptyState';
import AccountStrip from './AccountStrip';
import NetWorthChart from './NetWorthChart';
import SpendingChart from './SpendingChart';
import { cashAccounts, filterSnapshots, filterTransactions, formatMoney, latestUpdatedDate, upcomingObligations, type DateRange } from './view-utils';
import type { RouteId } from '../layout/AppShell';

export default function OverviewPage({ dataset, dateRange, privacyVisible, onTogglePrivacy, onNavigate = () => undefined }: { dataset: FinancialDataset; dateRange: DateRange; privacyVisible: boolean; onTogglePrivacy: () => void; onNavigate?: (route: RouteId) => void }) {
  const transactions = filterTransactions(dataset.transactions, dateRange);
  const cash = cashAccounts(dataset.accounts).reduce((total, account) => total + account.balanceCents, 0);
  const investments = investmentValueCents(dataset);
  const debt = debtCents(dataset);
  const snapshots = filterSnapshots(dataset.netWorthSnapshots ?? [], dateRange);
  const netWorthChange = snapshots.length > 1 ? snapshots.at(-1)!.netWorthCents - snapshots.at(-2)!.netWorthCents : undefined;
  const obligations = upcomingObligations(dataset.recurringObligations, snapshots.at(-1)?.date ?? new Date().toISOString().slice(0, 10));
  const metric = (value: number, label: string) => <PrivacyValue value={formatMoney(value)} visible={privacyVisible} onToggle={onTogglePrivacy} label={label} />;
  return <>
    <div className="page-intro"><div><p className="page-kicker">Signal & control</p><h1>Good evening.</h1><p className="page-subtitle">Here is your financial signal for today.</p></div><span className="status-chip"><span aria-hidden="true">●</span> Local data</span></div>
    <SectionHeader title="Overview" eyebrow="Financial signal" description={`Last updated ${latestUpdatedDate(dataset)} · ${dateRange === 'all' ? 'All time' : `Last ${dateRange} months`}`} />
    <div className="metric-grid metric-grid--wide"><MetricCard label="Net worth" value={metric(netWorthCents(dataset), 'net worth')} trend="Current position" /><MetricCard label="Net worth change" value={netWorthChange === undefined ? 'Not enough data' : metric(netWorthChange, 'net worth change')} trend="Month over month" /><MetricCard label="Cash" value={metric(cash, 'cash')} trend={`${cashAccounts(dataset.accounts).length} accounts`} /><MetricCard label="Income" value={metric(sumIncome(transactions), 'income')} trend="Selected period" /><MetricCard label="Spending" value={metric(sumSpending(transactions), 'spending')} trend="Selected period" /><MetricCard label="Savings rate" value={<PrivacyValue value={`${Math.round(savingsRate(transactions) * 100)}%`} visible={privacyVisible} onToggle={onTogglePrivacy} label="savings rate" />} trend="Income less spending" /><MetricCard label="Investments" value={metric(investments, 'investments')} trend={`${dataset.investmentHoldings.length} holdings`} /><MetricCard label="Debt" value={metric(debt, 'debt')} trend={`${dataset.accounts.filter((account) => account.kind === 'credit' || account.kind === 'loan').length + dataset.liabilities.length} sources`} /><MetricCard label="Data updated" value={latestUpdatedDate(dataset)} trend="Latest local record" /></div>
     <div className="dashboard-chart-grid"><SpendingChart transactions={dataset.transactions} dateRange={dateRange} privacyVisible={privacyVisible} onImport={() => onNavigate('import')} /><NetWorthChart snapshots={dataset.netWorthSnapshots} dateRange={dateRange} privacyVisible={privacyVisible} onImport={() => onNavigate('import')} /></div>
    <SectionHeader title="Upcoming recurring obligations" eyebrow="Committed" description="The next scheduled outflows after the latest recorded snapshot." />
     {obligations.length === 0 ? <EmptyState title="No upcoming obligations" description="Add a recurring obligation to track future commitments." action={<button className="button" type="button" onClick={() => onNavigate('spending')}>Add obligation</button>} /> : <div className="data-list">{obligations.map((obligation) => <div className="data-row" key={obligation.id}><span>{obligation.name}<small>{obligation.nextDueDate} · {obligation.frequency}</small></span><strong>{metric(obligation.amountCents, `${obligation.name} obligation`)}</strong></div>)}</div>}
    <AccountStrip accounts={dataset.accounts} privacyVisible={privacyVisible} />
  </>;
}
