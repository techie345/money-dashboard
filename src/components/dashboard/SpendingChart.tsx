import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { monthlySpending } from '../../domain/calculations';
import type { Transaction } from '../../domain/types';
import EmptyState from '../ui/EmptyState';
import PrivacyValue from '../ui/PrivacyValue';
import { filterTransactions, formatMoney, type DateRange } from './view-utils';

export default function SpendingChart({ transactions, dateRange, privacyVisible = true, onImport = () => undefined }: { transactions: Transaction[]; dateRange: DateRange; privacyVisible?: boolean; onImport?: () => void }) {
  const data = Object.entries(monthlySpending(filterTransactions(transactions, dateRange)))
    .sort(([left], [right]) => left.localeCompare(right)).map(([month, amount]) => ({ month, amount: amount / 100 }));
  return (
    <section className="chart-panel" aria-label="Monthly spending chart">
      <h3>Monthly spending</h3>
      {data.length === 0 ? <><p className="chart-empty-title">No spending data</p><EmptyState title="No spending data" description="Import transactions to see monthly spending." action={<button className="button" type="button" onClick={onImport}>Import transactions</button>} /></> : <>
        <p className="chart-summary">Spending ranges from <PrivacyValue value={formatMoney(Math.min(...data.map((item) => item.amount * 100)))} visible={privacyVisible} label="minimum spending" /> to <PrivacyValue value={formatMoney(Math.max(...data.map((item) => item.amount * 100)))} visible={privacyVisible} label="maximum spending" /> per month.</p>
        <div className="chart-frame"><ResponsiveContainer width="100%" height={240}><BarChart data={data} accessibilityLayer><CartesianGrid stroke="var(--color-border)" strokeDasharray="3 3" /><XAxis dataKey="month" /><YAxis tickFormatter={(value) => privacyVisible ? `$${value / 1000}k` : '••••'} /><Tooltip formatter={(value) => privacyVisible ? formatMoney(Number(value) * 100) : '••••••'} /><Bar dataKey="amount" fill="var(--color-teal)" radius={[5, 5, 0, 0]} /></BarChart></ResponsiveContainer></div>
      </>}
    </section>
  );
}
