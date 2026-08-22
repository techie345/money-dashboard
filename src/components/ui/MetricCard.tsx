import type { ReactNode } from 'react';

export interface MetricCardProps {
  label: string;
  value: ReactNode;
  trend?: string;
  loading?: boolean;
  empty?: boolean;
  emptyMessage?: string;
  children?: ReactNode;
}

export default function MetricCard({ label, value, trend, loading = false, empty = false, emptyMessage = 'No data available', children }: MetricCardProps) {
  return (
    <article className="metric-card" aria-busy={loading}>
      <h3 className="metric-card__label">{label}</h3>
      {loading ? <p className="metric-card__loading" role="status">Loading...</p> : empty ? <p className="metric-card__empty" role="status">{emptyMessage}</p> : <p className="metric-card__value">{value}</p>}
      {trend && !loading && !empty && <p className="metric-card__trend">{trend}</p>}
      {children}
    </article>
  );
}
