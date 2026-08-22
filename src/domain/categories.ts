export const CATEGORIES = [
  'housing',
  'food',
  'transportation',
  'utilities',
  'healthcare',
  'shopping',
  'entertainment',
  'subscriptions',
  'salary',
  'other',
  'transfer',
] as const;

export type Category = (typeof CATEGORIES)[number];
