import { normalizeMerchantName } from '../domain/rules';
import type { InvestmentHolding, Transaction } from '../domain/types';

export interface DuplicateCandidate { incomingId: string; existingId: string; reason: 'source_id' | 'account_date_amount_merchant_kind'; }

function duplicateReason(next: Transaction, prior: Transaction): DuplicateCandidate['reason'] | undefined {
  if (next.accountId !== prior.accountId) return undefined;
  if (next.sourceId && prior.sourceId) return next.sourceId === prior.sourceId ? 'source_id' : undefined;
  if (next.date === prior.date && next.amountCents === prior.amountCents && normalizeMerchantName(next.merchant) === normalizeMerchantName(prior.merchant) && next.kind === prior.kind) return 'account_date_amount_merchant_kind';
  return undefined;
}

export function findDuplicateCandidates(incoming: Transaction[], existing: Transaction[]): DuplicateCandidate[] {
  const candidates: DuplicateCandidate[] = [];
  for (const next of incoming) {
    for (const prior of existing) {
      const reason = duplicateReason(next, prior);
      if (reason) candidates.push({ incomingId: next.id, existingId: prior.id, reason });
    }
  }
  return candidates;
}

export function findIncomingDuplicateIds(incoming: Transaction[]): Set<string> {
  const duplicateIds = new Set<string>();
  for (let index = 0; index < incoming.length; index += 1) {
    for (let priorIndex = 0; priorIndex < index; priorIndex += 1) {
      if (duplicateReason(incoming[index], incoming[priorIndex])) {
        duplicateIds.add(incoming[index].id);
        break;
      }
    }
  }
  return duplicateIds;
}

export function upsertHoldings(existing: InvestmentHolding[], incoming: InvestmentHolding[]): InvestmentHolding[] {
  const result = [...existing];
  for (const holding of incoming) {
    const index = result.findIndex((current) => current.accountId === holding.accountId && current.symbol.toLowerCase() === holding.symbol.toLowerCase());
    if (index === -1) result.push(holding);
    else result[index] = { ...holding, id: result[index].id };
  }
  return result;
}
