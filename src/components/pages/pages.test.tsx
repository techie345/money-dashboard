import { describe, expect, it } from 'vitest';
import { renderToStaticMarkup } from 'react-dom/server';
import { createElement } from 'react';
import { createDemoDataset } from '../../data/demo-data';
import SpendingPage from './SpendingPage';
import AccountsPage from './AccountsPage';
import InvestingPage from './InvestingPage';
import PropertyPage from './PropertyPage';
import VehiclesPage from './VehiclesPage';
import IncomePage from './IncomePage';
import ImportPage from './ImportPage';
import SettingsPage from './SettingsPage';

describe('dedicated financial views', () => {
  const dataset = createDemoDataset();
  const props = { dataset, dateRange: 'all' as const, privacyVisible: true };

  it('renders category spending and recurring obligations', () => {
    const rendered = renderToStaticMarkup(SpendingPage(props));
    expect(rendered).toContain('Food');
    expect(rendered).toContain('Recurring obligations');
  });

  it('renders accounts, holdings, property, vehicles, and income details', () => {
    expect(renderToStaticMarkup(AccountsPage(props))).toContain('Total Checking');
    expect(renderToStaticMarkup(InvestingPage(props))).toContain('VTI');
    expect(renderToStaticMarkup(PropertyPage(props))).toContain('Primary residence');
    expect(renderToStaticMarkup(VehiclesPage(props))).toContain('2022 Mazda CX-5');
    expect(renderToStaticMarkup(IncomePage(props))).toContain('Acme Payroll');
    expect(renderToStaticMarkup(IncomePage(props))).toContain('Liabilities');
  });

  it('filters property and vehicle costs by the selected range', () => {
    const datedDataset = { ...dataset, transactions: [...dataset.transactions, { ...dataset.transactions[0], id: 'old-housing', date: '2026-05-03', category: 'housing' as const }] };
    expect(renderToStaticMarkup(PropertyPage({ ...props, dataset: datedDataset, dateRange: '3' }))).toContain('2026-08-03');
    expect(renderToStaticMarkup(PropertyPage({ ...props, dataset: datedDataset, dateRange: '3' }))).not.toContain('2026-05-03');
    expect(renderToStaticMarkup(VehiclesPage({ ...props, dataset: datedDataset, dateRange: '3' }))).toContain('2026-08-10');
    expect(renderToStaticMarkup(VehiclesPage({ ...props, dataset: datedDataset, dateRange: '3' }))).not.toContain('2026-05-03');
  });

  it('shows empty views with contextual actions', () => {
    const empty = { ...dataset, accounts: [], transactions: [], assets: [], investmentHoldings: [] };
    expect(renderToStaticMarkup(SpendingPage({ ...props, dataset: empty }))).toContain('Import transactions');
    expect(renderToStaticMarkup(PropertyPage({ ...props, dataset: empty }))).toContain('Add property');
    expect(renderToStaticMarkup(VehiclesPage({ ...props, dataset: empty }))).toContain('Add vehicle');
    expect(renderToStaticMarkup(InvestingPage({ ...props, dataset: empty }))).toContain('Import investments');
    expect(renderToStaticMarkup(SpendingPage({ ...props, dataset: { ...empty, recurringObligations: [] } }))).toContain('Add obligation');
  });

  it('shows account-specific update dates and secured loan balances', () => {
    const renderedAccounts = renderToStaticMarkup(AccountsPage(props));
    expect(renderedAccounts).toContain('Updated 2026-08-01');
    expect(renderToStaticMarkup(PropertyPage(props))).toContain('Mortgage balance');
    expect(renderToStaticMarkup(PropertyPage(props))).toContain('$284,600');
    expect(renderToStaticMarkup(VehiclesPage(props))).toContain('Auto loan balance');
    expect(renderToStaticMarkup(VehiclesPage(props))).toContain('$18,240');
  });

  it('renders the import review workflow and validation categories', () => {
    const rendered = renderToStaticMarkup(createElement(ImportPage, { dataset, onDatasetChange: () => undefined }));
    expect(rendered).toContain('Upload a CSV');
    expect(rendered).toContain('Source profile');
    expect(rendered).toContain('Mapping and validation');
    expect(rendered).toContain('Duplicate candidates');
    expect(rendered).toContain('Validation errors');
    expect(rendered).toContain('Needs review');
  });

  it('renders export and destructive reset controls', () => {
    const rendered = renderToStaticMarkup(createElement(SettingsPage, { dataset, onDatasetChange: () => undefined }));
    expect(rendered).toContain('Export full dataset');
    expect(rendered).toContain('Export transactions CSV');
    expect(rendered).toContain('Clear all browser data');
  });

});
