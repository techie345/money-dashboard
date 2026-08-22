import { describe, expect, it } from 'vitest';
import App from './App';
import { createDemoDataset } from './data/demo-data';

describe('app smoke test', () => {
  it('renders the application shell', () => {
    expect(App({ dataset: createDemoDataset() })).toMatchObject({
      type: expect.any(Function),
      props: { 'data-account-count': 8 },
    });
  });
});
