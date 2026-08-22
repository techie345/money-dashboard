import { openDB, type DBSchema, type IDBPDatabase } from 'idb';
import type { FinancialDataset, MerchantRule } from '../domain/types';
import { CATEGORIES } from '../domain/categories';

const DB_NAME = 'interactive-moneys';
const STORE_NAME = 'dataset';
const DATASET_KEY = 'current';
const PREFERENCES_KEY = 'interactive-moneys:preferences';
const RULES_KEY = 'interactive-moneys:merchant-rules';

interface MoneyDatabase extends DBSchema {
  dataset: { key: string; value: FinancialDataset };
}

let database: Promise<IDBPDatabase<MoneyDatabase>> | undefined;
let connection: IDBPDatabase<MoneyDatabase> | undefined;

function openDatabase(): Promise<IDBPDatabase<MoneyDatabase>> {
  return openDB<MoneyDatabase>(DB_NAME, 1, {
    upgrade(db) { if (!db.objectStoreNames.contains(STORE_NAME)) db.createObjectStore(STORE_NAME); },
    blocked() { console.warn('Interactive Moneys database open is blocked by another tab.'); },
    blocking() {
      connection?.close();
      connection = undefined;
      database = undefined;
    },
    terminated() {
      connection = undefined;
      database = undefined;
    },
  }).then((db) => {
    connection = db;
    return db;
  });
}

function getDatabase(): Promise<IDBPDatabase<MoneyDatabase>> {
  if (!database) {
    database = openDatabase().catch((error: unknown) => {
      database = undefined;
      connection = undefined;
      throw error;
    });
  }
  return database;
}

export async function loadDataset(): Promise<FinancialDataset | undefined> {
  return (await getDatabase()).get(STORE_NAME, DATASET_KEY);
}

export async function saveDataset(dataset: FinancialDataset): Promise<void> {
  await (await getDatabase()).put(STORE_NAME, dataset, DATASET_KEY);
}

export async function clearDataset(): Promise<void> {
  await (await getDatabase()).delete(STORE_NAME, DATASET_KEY);
}

export type Preferences = Record<string, unknown>;

function readLocal<T>(key: string, fallback: T): T {
  try {
    const value = localStorage.getItem(key);
    return value === null ? fallback : JSON.parse(value) as T;
  } catch { return fallback; }
}

function writeLocal(key: string, value: unknown): boolean {
  try {
    localStorage.setItem(key, JSON.stringify(value));
    return true;
  } catch { return false; }
}

export const getPreferences = (): Preferences => {
  const preferences = readLocal<unknown>(PREFERENCES_KEY, {});
  return typeof preferences === 'object' && preferences !== null && !Array.isArray(preferences)
    ? preferences as Preferences
    : {};
};
export const savePreferences = (preferences: Preferences): boolean => writeLocal(PREFERENCES_KEY, preferences);

const transactionKinds = new Set(['spending', 'income', 'transfer', 'credit_card_payment', 'investment', 'refund']);
const isNonEmptyString = (value: unknown): value is string => typeof value === 'string' && value.trim().length > 0;
function isMerchantRule(value: unknown): value is MerchantRule {
  if (typeof value !== 'object' || value === null) return false;
  const rule = value as Partial<MerchantRule>;
  return isNonEmptyString(rule.id)
    && isNonEmptyString(rule.pattern)
    && isNonEmptyString(rule.category)
    && CATEGORIES.includes(rule.category as (typeof CATEGORIES)[number])
    && (rule.accountId === undefined || isNonEmptyString(rule.accountId))
    && (rule.kind === undefined || transactionKinds.has(rule.kind));
}

export const getMerchantRules = (): MerchantRule[] => {
  const rules = readLocal<unknown>(RULES_KEY, []);
  return Array.isArray(rules) ? rules.filter(isMerchantRule) : [];
};
export const saveMerchantRules = (rules: MerchantRule[]): boolean => writeLocal(RULES_KEY, rules);

export async function clearBrowserData(): Promise<void> {
  await clearDataset();
  try {
    localStorage.removeItem(PREFERENCES_KEY);
    localStorage.removeItem(RULES_KEY);
  } catch { /* Browser storage may be unavailable; IndexedDB was still cleared. */ }
}
