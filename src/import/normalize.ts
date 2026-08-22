import { applyMerchantRules, normalizeMerchantName } from '../domain/rules';
import type { MerchantRule, Transaction, TransactionDirection } from '../domain/types';
import type { CsvRow } from './csv';
import type { ParsedCsv } from './csv';
import { getProfile, type ProfileId, type SourceProfile } from './profiles';

export interface ImportPosition { id: string; accountId: string; symbol: string; description: string; shares: number; costBasisCents?: number; marketValueCents: number; allocation?: number; source?: string; sourceFile?: string; importedAt?: string; }
export interface ImportBalance { id: string; accountId: string; date: string; kind: 'auto_loan' | 'mortgage'; balanceCents: number; source?: string; sourceFile?: string; importedAt?: string; }
export interface ImportError { row: number; message: string; data?: Record<string, string>; }
export type ImportField = 'date' | 'description' | 'amount' | 'debit' | 'credit';
export type FieldMapping = Partial<Record<ImportField, string>>;
export type ImportCorrections = Record<number, Record<string, string>>;
export interface NormalizeOptions { profile: ProfileId | SourceProfile; accountId: string; sourceFile: string; importedAt?: string; fieldMapping?: FieldMapping; corrections?: ImportCorrections; }
export interface NormalizeResult { transactions: Transaction[]; positions: ImportPosition[]; balances: ImportBalance[]; errors: ImportError[]; importedAt?: string; matchedRules?: Record<string, MerchantRule>; }

const value = (data: Record<string, string>, ...names: string[]): string => {
  const wanted = names.map((name) => name.trim().toLowerCase().replace(/[^a-z0-9]+/g, ' ').trim());
  const key = Object.keys(data).find((candidate) => wanted.includes(candidate.trim().toLowerCase().replace(/[^a-z0-9]+/g, ' ').trim()) && (data[candidate] ?? '').trim().length > 0);
  return key ? (data[key] ?? '').trim() : '';
};
const cents = (input: string): number => {
  const cleaned = input.trim().replace(/[$,\s]/g, '').replace(/,/g, '');
  if (!cleaned || !/^-?(?:\d+(?:\.\d+)?|\.\d+)$/.test(cleaned.replace(/^\((.*)\)$/, '-$1'))) throw new Error(`invalid amount: ${input}`);
  const number = cleaned.startsWith('(') && cleaned.endsWith(')') ? -Number(cleaned.slice(1, -1)) : Number(cleaned);
  if (!Number.isFinite(number) || number === 0) throw new Error(`invalid amount: ${input}`);
  return Math.round(Math.abs(number) * 100);
};
const signedNumber = (input: string): number => {
  const cleaned = input.trim().replace(/[$,\s]/g, '').replace(/,/g, '').replace(/^\((.*)\)$/, '-$1');
  if (!cleaned || !/^-?(?:\d+(?:\.\d+)?|\.\d+)$/.test(cleaned)) throw new Error(`invalid amount: ${input}`);
  const number = Number(cleaned);
  if (!Number.isFinite(number) || number === 0) throw new Error(`invalid amount: ${input}`);
  return number;
};
function isoDate(input: string): string {
  const trimmed = input.trim();
  const match = /^(\d{1,2})[/-](\d{1,2})[/-](\d{4})$/.exec(trimmed);
  const iso = match ? `${match[3]}-${match[1].padStart(2, '0')}-${match[2].padStart(2, '0')}` : trimmed;
  if (!/^\d{4}-\d{2}-\d{2}$/.test(iso) || Number.isNaN(new Date(`${iso}T00:00:00Z`).getTime()) || new Date(`${iso}T00:00:00Z`).toISOString().slice(0, 10) !== iso) throw new Error(`invalid date: ${input}`);
  return iso;
}
const idFor = (profile: SourceProfile, row: CsvRow, sourceId: string): string => `${profile.id}-${sourceId || row.row}`;
const isTransfer = (text: string): boolean => /transfer|ach (?:debit|credit)|wire|zelle|venmo|cash app|payment to|payment received|online transfer|external transfer/i.test(text);
const isCardPayment = (text: string): boolean => /credit card payment|card payment|online bill pay.*card|bill pay.*card|payment (?:to|for).*(?:credit card|card)/i.test(text);
const isRefund = (text: string): boolean => /\b(?:refund(?:ed)?|returned|reversal)\b/i.test(text);
const kindFor = (text: string, direction: TransactionDirection, profile: SourceProfile): Transaction['kind'] => {
  if (profile.sourceType === 'investment_activity') return 'investment';
  if (isCardPayment(text)) return 'credit_card_payment';
  if (profile.sourceType === 'card' && isRefund(text)) return 'refund';
  if (isTransfer(text)) return 'transfer';
  return direction === 'inflow' ? 'income' : 'spending';
};
const cardLike = (profile: SourceProfile): boolean => profile.sourceType === 'card';
const withOverflow = (row: CsvRow, input: string): string => {
  const extra = row.data.__parsed_extra;
  return input && Array.isArray(extra) && extra.length > 0 ? `${input},${extra.join(',')}` : input;
};
const correctedRow = (row: CsvRow, corrections?: ImportCorrections, mapping?: FieldMapping): CsvRow => {
  const correction = corrections?.[row.row];
  if (!correction) return row;
  const data = { ...row.data };
  for (const [field, correctedValue] of Object.entries(correction)) data[mapping?.[field as ImportField] ?? field] = correctedValue;
  return { ...row, data };
};
export function correctImport(input: CsvRow[] | ParsedCsv, corrections: ImportCorrections): CsvRow[] | ParsedCsv {
  if (Array.isArray(input)) return input.map((row) => correctedRow(row, corrections));
  return { ...input, rows: input.rows.map((row) => correctedRow(row, corrections)) };
}
const mappedValue = (data: Record<string, string>, field: ImportField, mapping: FieldMapping | undefined, ...fallbacks: string[]): string => {
  return value(data, ...(mapping?.[field] ? [mapping[field] as string, ...fallbacks] : fallbacks));
};
const required = (input: string, field: string): string => {
  if (!input.trim()) throw new Error(`missing ${field}`);
  return input;
};

export function normalizeImport(input: CsvRow[] | ParsedCsv, options: NormalizeOptions): NormalizeResult {
  const profile = typeof options.profile === 'string' ? getProfile(options.profile) : options.profile;
  const result: NormalizeResult = { transactions: [], positions: [], balances: [], errors: [], importedAt: options.importedAt };
  const rows = (Array.isArray(input) ? input : input.rows).map((row) => correctedRow(row, options.corrections, options.fieldMapping));
  if (!Array.isArray(input)) result.errors.push(...input.errors);
  rows.forEach((row) => {
    try {
      if (profile.sourceType === 'investment_positions') {
        const symbol = required(value(row.data, 'symbol'), 'symbol');
        const shares = signedNumber(required(value(row.data, 'quantity'), 'quantity'));
        if (shares < 0) throw new Error(`invalid shares: ${shares}`);
        const marketValueCents = cents(required(withOverflow(row, value(row.data, 'current value')), 'current value'));
        const costBasisInput = value(row.data, 'cost basis total', 'cost basis');
        const costBasisCents = costBasisInput ? cents(withOverflow(row, costBasisInput)) : undefined;
        const allocationInput = value(row.data, '% of account', 'allocation', 'percent of account');
        const allocation = allocationInput ? Number(allocationInput.replace('%', '').trim()) : undefined;
        if (allocationInput && (allocation === undefined || !Number.isFinite(allocation) || allocation < 0 || allocation > 100)) throw new Error(`invalid allocation: ${allocationInput}`);
        result.positions.push({ id: `${profile.id}-${row.row}`, accountId: options.accountId, symbol, description: value(row.data, 'description'), shares, costBasisCents, marketValueCents, allocation, source: profile.institution, sourceFile: options.sourceFile, importedAt: options.importedAt });
        return;
      }
      if (profile.sourceType === 'loan_balance') {
        const date = isoDate(required(value(row.data, 'date'), 'date'));
        let balanceValue = value(row.data, profile.id === 'mazda-loan' ? 'balance' : 'unpaid principal balance');
        const extra = row.data.__parsed_extra;
        if (extra && balanceValue && Array.isArray(extra) && extra.length > 0) balanceValue = `${balanceValue},${extra[extra.length - 1]}`;
        balanceValue ||= Object.values(row.data).find((entry) => /^\d{1,3},\d{3}(?:\.\d+)?$/.test(entry.trim())) || '';
        result.balances.push({ id: `${profile.id}-${row.row}`, accountId: options.accountId, date, kind: profile.id === 'mazda-loan' ? 'auto_loan' : 'mortgage', balanceCents: cents(required(balanceValue, 'balance')), source: profile.institution, sourceFile: options.sourceFile, importedAt: options.importedAt });
        return;
      }
      const date = isoDate(required(mappedValue(row.data, 'date', options.fieldMapping, profile.id === 'fidelity-activity' ? 'run date' : profile.id === 'discover-card' ? 'trans date' : 'date', 'posting date', 'transaction date'), 'date'));
      const description = mappedValue(row.data, 'description', options.fieldMapping, 'description', 'details') || 'Imported transaction';
      const debit = mappedValue(row.data, 'debit', options.fieldMapping, 'debit');
      const credit = mappedValue(row.data, 'credit', options.fieldMapping, 'credit');
       const amount = mappedValue(row.data, 'amount', options.fieldMapping, 'amount');
       const rawAmount = profile.id === 'chase-checking' && (debit || credit) ? withOverflow(row, debit || credit) : profile.id === 'sofi-bank' || profile.sourceType === 'generic' && (debit || credit) ? withOverflow(row, debit || credit) : withOverflow(row, amount);
       const parsedAmount = signedNumber(required(rawAmount, 'amount'));
       const signed = profile.id === 'chase-checking' ? (credit ? Math.abs(signedNumber(required(withOverflow(row, credit), 'credit'))) : debit ? -Math.abs(signedNumber(required(withOverflow(row, debit), 'debit'))) : (value(row.data, 'type').toLowerCase().includes('credit') ? Math.abs(parsedAmount) : -Math.abs(parsedAmount))) : profile.id === 'sofi-bank' || profile.sourceType === 'generic' && (debit || credit) ? (credit ? Math.abs(signedNumber(required(withOverflow(row, credit), 'credit'))) : -Math.abs(signedNumber(required(withOverflow(row, debit), 'debit')))) : cardLike(profile) ? (isRefund(description) || isCardPayment(description) ? Math.abs(parsedAmount) : profile.id === 'discover-card' ? -Math.abs(parsedAmount) : parsedAmount) : parsedAmount;
      const direction: TransactionDirection = signed < 0 ? 'outflow' : 'inflow';
      const kind = kindFor(description, direction, profile);
      const sourceId = value(row.data, 'transaction id', 'reference number', 'id', 'check or slip') || undefined;
      const action = value(row.data, 'action');
      const symbol = value(row.data, 'symbol');
      const quantityInput = value(row.data, 'quantity');
      const priceInput = value(row.data, 'price');
      result.transactions.push({ id: idFor(profile, row, sourceId ?? ''), date, merchant: description, description, amountCents: cents(String(signed)), kind, category: kind === 'transfer' || kind === 'investment' || kind === 'credit_card_payment' ? 'transfer' : kind === 'income' ? 'salary' : 'other', accountId: options.accountId, source: profile.institution, sourceFile: options.sourceFile, reviewStatus: 'needs_review', categorySource: 'imported', importedAt: options.importedAt, ...(sourceId ? { sourceId } : {}), direction, ...(action ? { action } : {}), ...(symbol ? { symbol } : {}), ...(quantityInput ? { shares: signedNumber(quantityInput) } : {}), ...(priceInput ? { priceCents: cents(priceInput) } : {}), ...(kind === 'credit_card_payment' ? { transferType: 'credit_card_payment' as const } : kind === 'transfer' ? { transferType: 'account_transfer' as const } : {}) });
    } catch (error) {
      result.errors.push({ row: row.row, message: error instanceof Error ? error.message : String(error), data: row.data });
    }
  });
  return result;
}

export function applyImportRules(result: NormalizeResult, rules: MerchantRule[]): NormalizeResult {
  const applied = applyMerchantRules(result.transactions, rules);
  return { ...result, transactions: applied.transactions, matchedRules: applied.matchedRules };
}

export { normalizeMerchantName };
