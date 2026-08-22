import type { ReactNode } from 'react';

export interface EmptyStateProps {
  title: string;
  description: string;
  action?: ReactNode;
}

export default function EmptyState({ title, description, action }: EmptyStateProps) {
  return <div className="empty-state" role="status"><span className="empty-state__signal" aria-hidden="true">+</span><h3>{title}</h3><p>{description}</p>{action}</div>;
}
