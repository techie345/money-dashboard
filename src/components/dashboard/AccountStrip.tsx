import type { Account } from '../../domain/types';
import PrivacyValue from '../ui/PrivacyValue';
import { formatMoney } from './view-utils';

export default function AccountStrip({ accounts, privacyVisible = true }: { accounts: Account[]; privacyVisible?: boolean }) {
  return <section className="account-strip" aria-labelledby="account-strip-heading"><h3 id="account-strip-heading">Accounts at a glance</h3><div className="account-strip__items">{accounts.map((account) => <article className="account-tile" key={account.id}><p>{account.institution}</p><h4>{account.name}</h4><PrivacyValue value={formatMoney(account.balanceCents)} visible={privacyVisible} label={`${account.name} balance`} /></article>)}</div></section>;
}
