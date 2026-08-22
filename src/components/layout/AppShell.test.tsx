import { describe, expect, it } from 'vitest';
import AppShell, { DateRangeControls, type NavigationItem } from './AppShell';
import Sidebar from './Sidebar';
import MobileNav from './MobileNav';

const navigation: NavigationItem[] = [
  { id: 'overview', label: 'Overview' },
  { id: 'spending', label: 'Spending' },
  { id: 'accounts', label: 'Accounts' },
  { id: 'investing', label: 'Investing' },
  { id: 'property', label: 'Property' },
  { id: 'vehicles', label: 'Vehicles' },
  { id: 'income', label: 'Income' },
  { id: 'import', label: 'Import' },
  { id: 'settings', label: 'Settings' },
];

describe('application shell navigation', () => {
  it('renders navigation landmarks and the active page', () => {
    const view = AppShell({
      activeRoute: 'overview',
      navigation,
      onNavigate: () => undefined,
      children: <h1>Overview</h1>,
    });

    expect(view.props.className).toBe('app-shell');
    expect(view.props.children).toHaveLength(3);
    expect(Sidebar({ activeRoute: 'overview', navigation, onNavigate: () => undefined }).props['aria-label']).toBe('Primary navigation');
    expect(MobileNav({ activeRoute: 'overview', navigation, onNavigate: () => undefined }).props['aria-label']).toBe('Mobile navigation');
  });

  it('marks the current navigation item and calls onNavigate from a button', () => {
    let selected = '';
    const view = Sidebar({ activeRoute: 'overview', navigation, onNavigate: (route) => { selected = route; } });
    const inner = view.props.children as { props: { children: Array<{ props: { children: unknown } }> } };
    const nav = inner.props.children[2] as { props: { children: Array<{ props: { 'aria-current'?: string; onClick: () => void } }> } };
    const buttons = nav.props.children;

    expect(buttons[0].props['aria-current']).toBe('page');
    buttons[1].props.onClick();
    expect(selected).toBe('spending');
  });

  it('keeps every route reachable from mobile navigation', () => {
    const view = MobileNav({ activeRoute: 'overview', navigation, onNavigate: () => undefined });
    const buttons = view.props.children as Array<{ props: { children: unknown } }>;

    expect(buttons).toHaveLength(navigation.length);
    expect(buttons.map((button) => (button.props.children as Array<{ props: { children: string } }>)[1].props.children)).toEqual(
      navigation.map((item) => item.label),
    );
  });

  it('renders date range controls and reports the selected range', () => {
    let selected = '';
    const controls = DateRangeControls({ dateRange: '6', onChange: (range) => { selected = range; } });
    const buttons = controls.props.children[1] as Array<{ props: { onClick: () => void; 'aria-pressed': boolean } }>;

    expect(buttons).toHaveLength(4);
    expect(buttons[1].props['aria-pressed']).toBe(true);
    buttons[0].props.onClick();
    expect(selected).toBe('3');
  });
});
