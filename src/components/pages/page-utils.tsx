import type { ReactNode } from 'react';
import type { DateRange } from '../dashboard/view-utils';
import SectionHeader from '../ui/SectionHeader';
import { formatMoney, titleCase } from '../dashboard/view-utils';
import PrivacyValue from '../ui/PrivacyValue';

export const PageFrame = ({ title, eyebrow, description, children }: { title: string; eyebrow: string; description: string; children: ReactNode }) => <>
  <div className="page-intro"><div><p className="page-kicker">{eyebrow}</p><h1>{title}</h1><p className="page-subtitle">{description}</p></div></div>
  {children}
</>;

export const money = (cents: number, visible: boolean) => <PrivacyValue value={formatMoney(cents)} visible={visible} />;
export const RangeNote = ({ dateRange }: { dateRange: DateRange }) => <p className="range-note">{dateRange === 'all' ? 'All recorded data' : `Last ${dateRange} months`} · values remain on this device</p>;
export const Label = ({ value }: { value: string }) => titleCase(value);
