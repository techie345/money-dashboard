import type { FinancialDataset } from './domain/types';
import { useState } from 'react';
import type { ReactNode } from 'react';
import AppShell, { type NavigationItem, type RouteId } from './components/layout/AppShell';
import OverviewPage from './components/dashboard/OverviewPage';
import SpendingPage from './components/pages/SpendingPage';
import AccountsPage from './components/pages/AccountsPage';
import InvestingPage from './components/pages/InvestingPage';
import PropertyPage from './components/pages/PropertyPage';
import VehiclesPage from './components/pages/VehiclesPage';
import IncomePage from './components/pages/IncomePage';
import ImportPage from './components/pages/ImportPage';
import SettingsPage from './components/pages/SettingsPage';
import type { DateRange } from './components/dashboard/view-utils';
import './styles/tokens.css';
import './styles/global.css';

export interface AppProps {
  dataset?: FinancialDataset;
  loading?: boolean;
  error?: string;
}

export function getCashAccountCount(dataset: FinancialDataset): number {
  return dataset.accounts.filter((account) => account.kind === 'cash').length;
}

const navigation: readonly NavigationItem[] = [
  { id: 'overview', label: 'Overview', shortLabel: 'OV' },
  { id: 'spending', label: 'Spending', shortLabel: 'SP' },
  { id: 'accounts', label: 'Accounts', shortLabel: 'AC' },
  { id: 'investing', label: 'Investing', shortLabel: 'IN' },
  { id: 'property', label: 'Property', shortLabel: 'PR' },
  { id: 'vehicles', label: 'Vehicles', shortLabel: 'VE' },
  { id: 'income', label: 'Income', shortLabel: 'IC' },
  { id: 'import', label: 'Import', shortLabel: 'IM' },
  { id: 'settings', label: 'Settings', shortLabel: 'ST' },
];

export default function App({ dataset, loading = false, error }: AppProps = {}) {
  if (loading) return <AppStateShell role="status">Loading your money...</AppStateShell>;
  if (error) return <AppStateShell role="alert">Unable to load your money: {error}</AppStateShell>;
  if (!dataset) return <AppStateShell role="alert">Unable to load your money: no dataset is available.</AppStateShell>;

  return <DashboardApp dataset={dataset} data-account-count={dataset.accounts.length} />;
}

function AppStateShell({ role, children }: { role: 'status' | 'alert'; children: ReactNode }) {
  return (
    <AppShell activeRoute="overview" navigation={navigation} onNavigate={() => undefined}>
      <section className="state-panel" role={role}>
        <p className="page-kicker">Financial workspace</p>
        <h1>{children}</h1>
      </section>
    </AppShell>
  );
}

function DashboardApp({ dataset, 'data-account-count': dataAccountCount }: { dataset: FinancialDataset; 'data-account-count'?: number }) {
  const [currentDataset, setCurrentDataset] = useState(dataset);
  const [activeRoute, setActiveRoute] = useState<RouteId>('overview');
  const [privacyVisible, setPrivacyVisible] = useState(true);
  const [dateRange, setDateRange] = useState<DateRange>('all');

  const activeItem = navigation.find((item) => item.id === activeRoute);
  const pageProps = { dataset: currentDataset, dateRange, privacyVisible, onNavigate: setActiveRoute };
  const content = activeRoute === 'overview' ? <OverviewPage {...pageProps} onTogglePrivacy={() => setPrivacyVisible((value) => !value)} />
    : activeRoute === 'spending' ? <SpendingPage {...pageProps} /> : activeRoute === 'accounts' ? <AccountsPage {...pageProps} />
      : activeRoute === 'investing' ? <InvestingPage {...pageProps} /> : activeRoute === 'property' ? <PropertyPage {...pageProps} />
        : activeRoute === 'vehicles' ? <VehiclesPage {...pageProps} /> : activeRoute === 'income' ? <IncomePage {...pageProps} />
          : activeRoute === 'import' ? <ImportPage dataset={currentDataset} onDatasetChange={setCurrentDataset} />
            : activeRoute === 'settings' ? <SettingsPage dataset={currentDataset} onDatasetChange={setCurrentDataset} />
              : <section className="route-placeholder" aria-labelledby="page-heading"><p className="page-kicker">Financial workspace</p><h1 id="page-heading">{activeItem?.label ?? 'Overview'}</h1><p>This workspace is ready for your local financial data.</p><button className="button" type="button" onClick={() => setActiveRoute('overview')}>Back to overview</button></section>;

  return <AppShell activeRoute={activeRoute} navigation={navigation} onNavigate={setActiveRoute} privacyVisible={privacyVisible} onTogglePrivacy={() => setPrivacyVisible((value) => !value)} dateRange={dateRange} onDateRangeChange={setDateRange} dataAccountCount={dataAccountCount}>{content}</AppShell>;
}
