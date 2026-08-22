import type { ReactNode } from 'react';
import MobileNav from './MobileNav';
import Sidebar from './Sidebar';
import type { DateRange } from '../dashboard/view-utils';

export const routeIds = ['overview', 'spending', 'accounts', 'investing', 'property', 'vehicles', 'income', 'import', 'settings'] as const;
export type RouteId = typeof routeIds[number];

export interface NavigationItem {
  id: RouteId;
  label: string;
  shortLabel?: string;
}

export interface AppShellProps {
  activeRoute: RouteId;
  navigation: readonly NavigationItem[];
  onNavigate: (route: RouteId) => void;
  children: ReactNode;
  privacyVisible?: boolean;
  onTogglePrivacy?: () => void;
  dataAccountCount?: number;
  dateRange?: DateRange;
  onDateRangeChange?: (range: DateRange) => void;
}

export function DateRangeControls({ dateRange, onChange }: { dateRange: DateRange; onChange: (range: DateRange) => void }) {
  return <div className="date-range-controls" aria-label="Date range"><span>Range</span>{(['3', '6', '12', 'all'] as const).map((range) => <button key={range} type="button" aria-pressed={dateRange === range} onClick={() => onChange(range)}>{range === 'all' ? 'All' : `${range} mo`}</button>)}</div>;
}

export default function AppShell({ activeRoute, navigation, onNavigate, children, privacyVisible = true, onTogglePrivacy, dataAccountCount, dateRange = 'all', onDateRangeChange }: AppShellProps) {
  return (
    <div className="app-shell" data-account-count={dataAccountCount}>
      <Sidebar activeRoute={activeRoute} navigation={navigation} onNavigate={onNavigate} />
      <main className="app-main" data-privacy-visible={privacyVisible}>
        {onTogglePrivacy ? <><button className="privacy-toggle" type="button" onClick={onTogglePrivacy} aria-pressed={privacyVisible}>
          <span aria-hidden="true">{privacyVisible ? '◉' : '◌'}</span> {privacyVisible ? 'Hide values' : 'Show values'}
        </button>{onDateRangeChange && <DateRangeControls dateRange={dateRange} onChange={onDateRangeChange} />}</> : onDateRangeChange && <DateRangeControls dateRange={dateRange} onChange={onDateRangeChange} />}
        {children}
      </main>
      <MobileNav activeRoute={activeRoute} navigation={navigation} onNavigate={onNavigate} />
    </div>
  );
}
