import { loadOrSeedDataset } from './seed';
import type { FinancialDataset } from '../domain/types';

interface BootstrapCallbacks {
  onSuccess: (dataset: FinancialDataset) => void;
  onError: (error: unknown) => void;
}

export function createDatasetBootstrap(loader: () => Promise<FinancialDataset>) {
  let pending: Promise<FinancialDataset> | undefined;

  const load = () => pending ??= loader();
  const start = ({ onSuccess, onError }: BootstrapCallbacks) => {
    let active = true;
    load().then(
      (dataset) => { if (active) onSuccess(dataset); },
      (error: unknown) => { if (active) onError(error); },
    );
    return () => { active = false; };
  };

  return { load, start };
}

export const appDatasetBootstrap = createDatasetBootstrap(loadOrSeedDataset);
