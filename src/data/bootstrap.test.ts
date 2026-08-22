import { describe, expect, it } from 'vitest';
import type { FinancialDataset } from '../domain/types';
import { createDatasetBootstrap } from './bootstrap';

const dataset = {} as FinancialDataset;

describe('dataset bootstrap lifecycle', () => {
  it('shares one load promise across StrictMode-style starts', async () => {
    let calls = 0;
    const bootstrap = createDatasetBootstrap(async () => {
      calls += 1;
      return dataset;
    });
    const first = bootstrap.load();
    const second = bootstrap.load();

    await expect(Promise.all([first, second])).resolves.toEqual([dataset, dataset]);
    expect(calls).toBe(1);
  });

  it('does not publish a late result after cancellation', async () => {
    let resolveLoad!: (value: FinancialDataset) => void;
    const bootstrap = createDatasetBootstrap(() => new Promise((resolve) => { resolveLoad = resolve; }));
    const results: FinancialDataset[] = [];
    const cancel = bootstrap.start({ onSuccess: (value) => results.push(value), onError: () => undefined });
    cancel();
    resolveLoad(dataset);
    await bootstrap.load();

    expect(results).toEqual([]);
  });

  it('publishes persistence errors to the error callback', async () => {
    const failure = new Error('persistence unavailable');
    const bootstrap = createDatasetBootstrap(async () => { throw failure; });
    const errors: unknown[] = [];

    bootstrap.start({ onSuccess: () => undefined, onError: (error) => errors.push(error) });
    await expect(bootstrap.load()).rejects.toBe(failure);
    expect(errors).toEqual([failure]);
  });
});
