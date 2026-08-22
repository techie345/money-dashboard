import { describe, expect, it } from 'vitest';
import { renderToStaticMarkup } from 'react-dom/server';
import { createDemoDataset } from '../../data/demo-data';
import OverviewPage from './OverviewPage';
import SpendingChart from './SpendingChart';
import NetWorthChart from './NetWorthChart';
import AccountStrip from './AccountStrip';
import { filterSnapshots, filterTransactions, upcomingObligations } from './view-utils';

describe('dashboard views', () => {
  const dataset = createDemoDataset();

  it('shows the complete overview signal and update dates', () => {
    const view = OverviewPage({ dataset, dateRange: 'all', privacyVisible: true, onTogglePrivacy: () => undefined });
    const rendered = renderToStaticMarkup(view);

    expect(rendered).toContain('Net worth');
    expect(rendered).toContain('Cash');
    expect(rendered).toContain('Income');
    expect(rendered).toContain('Spending');
    expect(rendered).toContain('Savings rate');
    expect(rendered).toContain('Investments');
    expect(rendered).toContain('Debt');
    expect(rendered).toContain('Last updated');
  });

  it('gives charts accessible summaries and an empty state', () => {
    const spending = SpendingChart({ transactions: [], dateRange: 'all' });
    const netWorth = NetWorthChart({ snapshots: [], dateRange: 'all' });

    expect(renderToStaticMarkup(spending)).toContain('No spending data');
    expect(renderToStaticMarkup(netWorth)).toContain('No net worth data');
    expect((spending.props as { 'aria-label'?: string })['aria-label']).toContain('spending');
    expect((netWorth.props as { 'aria-label'?: string })['aria-label']?.toLowerCase()).toContain('net worth');
  });

  it('renders account balances in the account strip', () => {
    const view = AccountStrip({ accounts: dataset.accounts, privacyVisible: true });
    const rendered = renderToStaticMarkup(view);
    expect(rendered).toContain('Chase');
    expect(rendered).toContain('SoFi');
    expect(rendered).toContain('Fidelity');
  });

  it('sorts snapshots and selects upcoming obligations', () => {
    expect(filterSnapshots([
      { date: '2026-08-21', assetsCents: 1, liabilitiesCents: 0, netWorthCents: 1 },
      { date: '2026-06-30', assetsCents: 1, liabilitiesCents: 0, netWorthCents: 1 },
    ], 'all').map((snapshot) => snapshot.date)).toEqual(['2026-06-30', '2026-08-21']);
    expect(upcomingObligations(dataset.recurringObligations, '2026-08-21').map((item) => item.name)).toEqual([
      'Mortgage', 'Mazda payment', 'Fiber Internet', 'Cloud storage',
    ]);
  });

  it('uses the first day of the inclusive month window as its cutoff', () => {
    const transactions = [
      { ...dataset.transactions[0], id: 'june-end', date: '2026-06-30' },
      { ...dataset.transactions[0], id: 'may-end', date: '2026-05-31' },
      { ...dataset.transactions[0], id: 'august-end', date: '2026-08-31' },
    ];

    expect(filterTransactions(transactions, '3').map((transaction) => transaction.id)).toEqual([
      'june-end', 'august-end',
    ]);
  });

  it('masks overview chart numbers and savings rate when privacy is enabled', () => {
    const rendered = renderToStaticMarkup(OverviewPage({ dataset, dateRange: 'all', privacyVisible: false, onTogglePrivacy: () => undefined }));
    expect(rendered).toContain('••••••');
    expect(rendered).not.toContain('59%');
    expect(rendered).not.toContain('$225,488');
  });
});
