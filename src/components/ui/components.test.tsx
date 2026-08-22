import { describe, expect, it } from 'vitest';
import EmptyState from './EmptyState';
import MetricCard from './MetricCard';
import PrivacyValue from './PrivacyValue';
import SectionHeader from './SectionHeader';

describe('visual system primitives', () => {
  const text = (node: unknown): string => {
    if (typeof node === 'string') return node;
    if (Array.isArray(node)) return node.map(text).join('');
    if (node && typeof node === 'object' && 'props' in node) return text((node as { props: { children?: unknown } }).props.children);
    return '';
  };

  it('masks private values and exposes an accessible toggle', () => {
    let visible = false;
    const view = PrivacyValue({ value: '$12,345', visible, onToggle: () => { visible = !visible; } });
    expect(view.props.children).toContain('••••••');
    view.props.onClick();
    expect(visible).toBe(true);
    expect(view.props['aria-label']).toContain('Show');
  });

  it('supports metric loading and empty states', () => {
    expect(MetricCard({ label: 'Cash', value: '$12', loading: true }).props['aria-busy']).toBe(true);
    expect(text(MetricCard({ label: 'Cash', value: '$12', empty: true, emptyMessage: 'Import an account' }))).toContain('Import an account');
  });

  it('gives each metric card a semantic heading for its accessible name', () => {
    const view = MetricCard({ label: 'Cash position', value: '$12' });
    const children = view.props.children as Array<{ type: string; props: { children: string } }>;

    expect(children[0].type).toBe('h3');
    expect(children[0].props.children).toBe('Cash position');
  });

  it('renders semantic section headings and empty state status', () => {
    expect(text(SectionHeader({ title: 'Accounts', eyebrow: 'Balances' }))).toContain('Accounts');
    expect(EmptyState({ title: 'No accounts', description: 'Import a CSV to begin.' }).props.role).toBe('status');
  });
});
