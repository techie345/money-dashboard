import type { ReactNode } from 'react';

export interface SectionHeaderProps {
  title: string;
  eyebrow?: string;
  description?: string;
  action?: ReactNode;
}

export default function SectionHeader({ title, eyebrow, description, action }: SectionHeaderProps) {
  return (
    <header className="section-header">
      <div>{eyebrow && <p className="section-header__eyebrow">{eyebrow}</p>}<h2>{title}</h2>{description && <p className="section-header__description">{description}</p>}</div>
      {action && <div>{action}</div>}
    </header>
  );
}
