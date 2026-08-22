import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import type { NetWorthSnapshot } from '../../domain/types';
import EmptyState from '../ui/EmptyState';
import { filterSnapshots, formatMoney, type DateRange } from './view-utils';
import PrivacyValue from '../ui/PrivacyValue';

export default function NetWorthChart({ snapshots = [], dateRange, privacyVisible = true, onImport = () => undefined }: { snapshots?: NetWorthSnapshot[]; dateRange: DateRange; privacyVisible?: boolean; onImport?: () => void }) {
  const data = filterSnapshots(snapshots, dateRange).map((snapshot) => ({ date: snapshot.date, value: snapshot.netWorthCents / 100 }));
  return (
    <section className="chart-panel" aria-label="Net worth trend chart">
      <h3>Net worth trend</h3>
      {data.length === 0 ? <><p className="chart-empty-title">No net worth data</p><EmptyState title="No net worth data" description="Net worth snapshots will appear after an import." action={<button className="button" type="button" onClick={onImport}>Import net worth snapshots</button>} /></> : <>
        <p className="chart-summary">Net worth is <PrivacyValue value={formatMoney(data.at(-1)?.value ? data.at(-1)!.value * 100 : 0)} visible={privacyVisible} label="latest net worth" /> at the latest snapshot.</p>
        <div className="chart-frame"><ResponsiveContainer width="100%" height={240}><AreaChart data={data} accessibilityLayer><CartesianGrid stroke="var(--color-border)" strokeDasharray="3 3" /><XAxis dataKey="date" /><YAxis tickFormatter={(value) => privacyVisible ? `$${Math.round(value / 1000)}k` : '••••'} /><Tooltip formatter={(value) => privacyVisible ? formatMoney(Number(value) * 100) : '••••••'} /><Area type="monotone" dataKey="value" stroke="var(--color-cyan)" fill="var(--color-cyan)" fillOpacity={0.15} /></AreaChart></ResponsiveContainer></div>
      </>}
    </section>
  );
}
