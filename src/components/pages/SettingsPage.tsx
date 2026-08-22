import { useState } from 'react';
import type { FinancialDataset } from '../../domain/types';
import { clearBrowserData, saveDataset } from '../../data/storage';
import { createDemoDataset } from '../../data/demo-data';

export interface SettingsPageProps { dataset: FinancialDataset; onDatasetChange: (dataset: FinancialDataset) => void; }

export function transactionsCsv(dataset: FinancialDataset): string {
  const headers = ['date', 'merchant', 'description', 'amountCents', 'kind', 'category', 'accountId', 'source', 'sourceFile', 'reviewStatus', 'sourceId'];
  const quote = (value: unknown) => {
    const text = String(value ?? '');
    const safe = /^[=+\-@]/.test(text) ? `'${text}` : text;
    return `"${safe.replace(/"/g, '""')}"`;
  };
  return [headers.join(','), ...dataset.transactions.map((transaction) => headers.map((header) => quote(transaction[header as keyof typeof transaction])).join(','))].join('\n');
}

function download(name: string, content: string, type: string) {
  const url = URL.createObjectURL(new Blob([content], { type }));
  const link = document.createElement('a');
  link.href = url;
  link.download = name;
  link.click();
  URL.revokeObjectURL(url);
}

export default function SettingsPage({ dataset, onDatasetChange }: SettingsPageProps) {
  const [message, setMessage] = useState('');
  async function clearAndReseed() {
    if (!window.confirm('Clear all browser data and restore the demo dataset?')) return;
    try {
      await clearBrowserData();
      const demo = createDemoDataset();
      await saveDataset(demo);
      onDatasetChange(demo);
      setMessage('Browser data cleared and demo data restored.');
    } catch (error) {
      setMessage(`Could not reset browser data: ${error instanceof Error ? error.message : String(error)}`);
    }
  }
  return <div className="page-stack"><header className="page-intro"><div><p className="page-kicker">Local controls</p><h1>Settings</h1><p className="page-subtitle">Your data stays in this browser.</p></div></header>
    <section className="signal-panel" aria-labelledby="export-heading"><div className="section-header"><div><p className="section-header__eyebrow">Backup</p><h2 id="export-heading">Export data</h2></div></div><p className="page-subtitle">Download a portable copy of the current normalized dataset.</p><div className="button-row"><button className="button" type="button" onClick={() => download('interactive-moneys.json', JSON.stringify(dataset, null, 2), 'application/json')}>Export full dataset</button><button className="button button--secondary" type="button" onClick={() => download('interactive-moneys-transactions.csv', transactionsCsv(dataset), 'text/csv')}>Export transactions CSV</button></div></section>
     <section className="signal-panel danger-panel" aria-labelledby="clear-heading"><div className="section-header"><div><p className="section-header__eyebrow">Destructive action</p><h2 id="clear-heading">Clear browser data</h2></div></div><p className="page-subtitle">This removes the local dataset and settings, then reseeds the demo dataset.</p><button className="button button--danger" type="button" onClick={() => void clearAndReseed()}>Clear all browser data</button>{message && <p className="error-message" role="alert">{message}</p>}</section>
  </div>;
}
