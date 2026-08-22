import type { FinancialDataset } from '../../domain/types';
import SectionHeader from '../ui/SectionHeader';
import type { DateRange } from '../dashboard/view-utils';
import { PageFrame, RangeNote, money } from './page-utils';
import EmptyState from '../ui/EmptyState';
import type { RouteId } from '../layout/AppShell';

export default function AccountsPage({ dataset, dateRange, privacyVisible, onNavigate = () => undefined }: { dataset: FinancialDataset; dateRange: DateRange; privacyVisible: boolean; onNavigate?: (route: RouteId) => void }) {
  return <PageFrame title="Accounts" eyebrow="Balances" description="Every connected balance, grouped by institution."><RangeNote dateRange={dateRange} /><SectionHeader title="Account balances" eyebrow="Institutions" />{dataset.accounts.length === 0 ? <EmptyState title="No accounts connected" description="Import an account statement to start tracking balances." action={<button className="button" type="button" onClick={() => onNavigate('import')}>Import account</button>} /> : <div className="data-list">{dataset.accounts.map((account) => <div className="data-row" key={account.id}><span>{account.institution} <small>{account.name} · Updated {account.lastImportedAt ?? 'Not yet'}</small></span><strong>{money(account.balanceCents, privacyVisible)}</strong></div>)}</div>}</PageFrame>;
}
