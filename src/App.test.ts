import { describe, expect, it } from 'vitest';
import App, { getCashAccountCount } from './App';
import { createDemoDataset } from './data/demo-data';

const renderShell = (props: Parameters<typeof App>[0]) => {
  const app = App(props);
  const shell = app.type(app.props);
  return shell.type(shell.props);
};

describe('App data states', () => {
  it('shows a loading status before the dataset is ready', () => {
    const view = renderShell({ loading: true });
    expect(view.props.className).toBe('app-shell');
    expect(view.props.children[1].props.children[1].props.role).toBe('status');
    expect(view.props.children[1].props.children[1].props.children[1].props.children).toBe('Loading your money...');
  });

  it('shows an error fallback instead of normal UI when loading fails', () => {
    const view = renderShell({ error: 'Storage unavailable' });
    expect(view.props.className).toBe('app-shell');
    expect(view.props.children[1].props.children[1].props.role).toBe('alert');
    expect(view.props.children[1].props.children[1].props.children[1].props.children).toContain('Storage unavailable');
  });

  it('represents the loaded dataset in the minimal app shell', () => {
    const dataset = createDemoDataset();
    const view = App({ dataset });
    expect(view).toMatchObject({
      props: { 'data-account-count': dataset.accounts.length },
    });
  });

  it('reports the actual number of cash accounts in the overview metric', () => {
    const dataset = createDemoDataset();
    dataset.accounts.push({ ...dataset.accounts[0], id: 'second-checking', kind: 'cash' });
    expect(getCashAccountCount(dataset)).toBe(3);
  });
});
