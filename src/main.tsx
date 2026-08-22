import { StrictMode, useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';
import { appDatasetBootstrap } from './data/bootstrap';
import type { FinancialDataset } from './domain/types';

const rootElement = document.getElementById('root');

if (!rootElement) {
  throw new Error('Unable to start Interactive Moneys: #root mount element is missing.');
}

function AppBootstrap() {
  const [dataset, setDataset] = useState<FinancialDataset>();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();

  useEffect(() => {
    return appDatasetBootstrap.start({
      onSuccess: (loadedDataset) => {
        setDataset(loadedDataset);
        setLoading(false);
      },
      onError: (reason: unknown) => {
        setError(reason instanceof Error ? reason.message : 'Storage is unavailable.');
        setLoading(false);
      },
    });
  }, []);

  return <App dataset={dataset} loading={loading} error={error} />;
}

createRoot(rootElement).render(<StrictMode><AppBootstrap /></StrictMode>);
