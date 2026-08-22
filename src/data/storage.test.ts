import { describe, expect, it, beforeEach } from 'vitest';
import 'fake-indexeddb/auto';
import {
  clearDataset,
  loadDataset,
  saveDataset,
  getMerchantRules,
  getPreferences,
  saveMerchantRules,
  savePreferences,
  clearBrowserData,
} from './storage';
import type { FinancialDataset, MerchantRule } from '../domain/types';
import { createDemoDataset } from './demo-data';

describe('IndexedDB dataset storage', () => {
  it('creates version 1 store, round trips a dataset, and clears it', async () => {
    await clearDataset();

    const database = await new Promise<IDBDatabase>((resolve, reject) => {
      const request = indexedDB.open('interactive-moneys');
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error);
    });

    expect(database.version).toBe(1);
    expect(database.objectStoreNames.contains('dataset')).toBe(true);
    database.close();

    const dataset = createDemoDataset();
    await saveDataset(dataset);
    await expect(loadDataset()).resolves.toEqual(dataset);
    await clearDataset();
    await expect(loadDataset()).resolves.toBeUndefined();
  });
});

describe('local storage helpers', () => {
  beforeEach(() => {
    const values = new Map<string, string>();
    Object.defineProperty(globalThis, 'localStorage', {
      configurable: true,
      value: {
        clear: () => values.clear(),
        getItem: (key: string) => values.get(key) ?? null,
        setItem: (key: string, value: string) => values.set(key, value),
        removeItem: (key: string) => values.delete(key),
      },
    });
  });

  it('round trips preferences and merchant rules', () => {
    const rules: MerchantRule[] = [{ id: 'rule-1', pattern: 'Market', category: 'food' }];
    savePreferences({ currency: 'USD', privacyMode: true });
    saveMerchantRules(rules);

    expect(getPreferences()).toEqual({ currency: 'USD', privacyMode: true });
    expect(getMerchantRules()).toEqual(rules);
  });

  it('clears the dataset and local browser settings together', async () => {
    await saveDataset(createDemoDataset());
    savePreferences({ currency: 'USD' });
    saveMerchantRules([{ id: 'rule-1', pattern: 'Market', category: 'food' }]);

    await clearBrowserData();

    await expect(loadDataset()).resolves.toBeUndefined();
    expect(getPreferences()).toEqual({});
    expect(getMerchantRules()).toEqual([]);
  });

  it('returns defaults for missing or malformed local storage values', () => {
    localStorage.setItem('interactive-moneys:preferences', '{bad json');
    localStorage.setItem('interactive-moneys:merchant-rules', '[]');

    expect(getPreferences()).toEqual({});
    expect(getMerchantRules()).toEqual([]);

    localStorage.setItem('interactive-moneys:preferences', '[]');
    localStorage.setItem('interactive-moneys:merchant-rules', '{}');
    expect(getPreferences()).toEqual({});
    expect(getMerchantRules()).toEqual([]);
  });

  it('rejects invalid merchant rules and reports localStorage write failures', () => {
    const validRule: MerchantRule = { id: 'rule-1', pattern: 'Market', category: 'food' };
    saveMerchantRules([validRule]);
    localStorage.setItem('interactive-moneys:merchant-rules', JSON.stringify([
      validRule,
      { id: 'bad', pattern: 'Unknown', category: 'not-a-category' },
      { id: 42, pattern: 'Broken', category: 'food' },
    ]));
    expect(getMerchantRules()).toEqual([validRule]);

    Object.defineProperty(globalThis, 'localStorage', {
      configurable: true,
      value: { getItem: () => null, setItem: () => { throw new Error('quota'); } },
    });
    expect(savePreferences({ currency: 'USD' })).toBe(false);
    expect(saveMerchantRules([validRule])).toBe(false);
  });

  it.each([
    { id: '', pattern: 'Market', category: 'food' },
    { id: 'rule-1', pattern: '', category: 'food' },
    { id: 'rule-1', pattern: 'Market', category: 'food', accountId: '' },
    { id: 'rule-1', pattern: 'Market', category: 'food', kind: 'not-a-kind' },
  ])('rejects merchant rules with invalid persisted values', (rule) => {
    localStorage.setItem('interactive-moneys:merchant-rules', JSON.stringify([rule]));

    expect(getMerchantRules()).toEqual([]);
  });
});
