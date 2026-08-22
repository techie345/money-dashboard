import type { FinancialDataset } from '../../domain/types';
import SectionHeader from '../ui/SectionHeader';
import type { DateRange } from '../dashboard/view-utils';
import { PageFrame, RangeNote, money } from './page-utils';
import EmptyState from '../ui/EmptyState';
import type { RouteId } from '../layout/AppShell';

export default function InvestingPage({ dataset, dateRange, privacyVisible, onNavigate = () => undefined }: { dataset: FinancialDataset; dateRange: DateRange; privacyVisible: boolean; onNavigate?: (route: RouteId) => void }) {
  const investmentAccounts = dataset.accounts.filter((account) => account.kind === 'investment' && !dataset.investmentHoldings.some((holding) => holding.accountId === account.id));
  const noInvestmentData = dataset.investmentHoldings.length === 0 && investmentAccounts.length === 0;
  return <PageFrame title="Investing" eyebrow="Capital" description="Holdings, cost basis, and current market value."><RangeNote dateRange={dateRange} /><SectionHeader title="Holdings" eyebrow="Portfolio" />{noInvestmentData ? <EmptyState title="No investments recorded" description="Import investment holdings or account balances to track portfolio value." action={<button className="button" type="button" onClick={() => onNavigate('import')}>Import investments</button>} /> : <><div className="data-list">{dataset.investmentHoldings.map((holding) => <div className="data-row" key={holding.id}><span><strong>{holding.symbol}</strong><small>{holding.shares} shares · cost {money(holding.costBasisCents, privacyVisible)}</small></span><strong>{money(holding.marketValueCents, privacyVisible)}</strong></div>)}{investmentAccounts.map((account) => <div className="data-row" key={account.id}><span>{account.institution}<small>{account.name} · account value</small></span><strong>{money(account.balanceCents, privacyVisible)}</strong></div>)}</div></>}</PageFrame>;
}
