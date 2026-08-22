import { normalizeHeader } from './csv';

export type ProfileId =
  | 'discover-card' | 'chase-card' | 'amex-card' | 'chase-checking' | 'sofi-bank'
  | 'fidelity-activity' | 'fidelity-positions' | 'mazda-loan' | 'newrez-mortgage' | 'generic';
export type ProfileSourceType = 'bank' | 'card' | 'investment_activity' | 'investment_positions' | 'loan_balance' | 'generic';

export interface SourceProfile {
  id: ProfileId;
  institution: string;
  sourceType: ProfileSourceType;
  recognizedHeaders: string[];
  accountKind: 'cash' | 'credit' | 'investment' | 'loan' | 'other';
  signConvention: string;
  parser: ProfileId;
}

const profile = (id: ProfileId, institution: string, sourceType: ProfileSourceType, accountKind: SourceProfile['accountKind'], recognizedHeaders: string[], signConvention: string): SourceProfile => ({ id, institution, sourceType, accountKind, recognizedHeaders, signConvention, parser: id });

export const profiles: SourceProfile[] = [
  profile('discover-card', 'Discover', 'card', 'credit', ['trans date', 'post date', 'description', 'amount'], 'amount is charge magnitude'),
  profile('chase-card', 'Chase', 'card', 'credit', ['transaction date', 'post date', 'description', 'amount'], 'negative is charge'),
  profile('amex-card', 'American Express', 'card', 'credit', ['date', 'description', 'amount'], 'negative is charge'),
  profile('chase-checking', 'Chase', 'bank', 'cash', ['posting date', 'description', 'amount', 'type', 'debit', 'credit'], 'debit is outflow, credit is inflow'),
  profile('sofi-bank', 'SoFi', 'bank', 'cash', ['date', 'description', 'debit', 'credit'], 'debit is outflow, credit is inflow'),
  profile('fidelity-activity', 'Fidelity', 'investment_activity', 'investment', ['run date', 'action', 'symbol', 'amount'], 'activity amount is signed'),
  profile('fidelity-positions', 'Fidelity', 'investment_positions', 'investment', ['symbol', 'quantity', 'current value'], 'values are positive snapshots'),
  profile('mazda-loan', 'Mazda Bank', 'loan_balance', 'loan', ['date', 'payment', 'balance'], 'balance is outstanding debt'),
  profile('newrez-mortgage', 'Newrez', 'loan_balance', 'loan', ['date', 'payment', 'unpaid principal balance'], 'balance is outstanding debt'),
  profile('generic', 'Generic CSV', 'generic', 'other', [], 'signed amount: negative outflow'),
];

export function getProfile(selection: ProfileId | string[], headers?: string[]): SourceProfile {
  if (Array.isArray(selection)) {
    const normalized = new Set(selection.map(normalizeHeader));
    let best = profiles[profiles.length - 1];
    let score = 0;
    for (const candidate of profiles.slice(0, -1)) {
      const candidateScore = candidate.recognizedHeaders.filter((header) => normalized.has(header)).length;
      const isUnambiguous = candidateScore >= 4 || (candidate.id === 'discover-card' && normalized.has('trans date')) || (candidate.id === 'fidelity-positions' && normalized.has('current value')) || (candidate.id === 'newrez-mortgage' && normalized.has('unpaid principal balance')) || (candidate.id === 'mazda-loan' && normalized.has('balance'));
      if (isUnambiguous && candidateScore > score) { best = candidate; score = candidateScore; }
    }
    return best;
  }
  return profiles.find((candidate) => candidate.id === selection) ?? profiles[profiles.length - 1];
}

export const detectProfile = getProfile;
