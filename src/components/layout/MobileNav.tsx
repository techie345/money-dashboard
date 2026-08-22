import type { RouteId, NavigationItem } from './AppShell';

export interface MobileNavProps {
  activeRoute: RouteId;
  navigation: readonly NavigationItem[];
  onNavigate: (route: RouteId) => void;
}

export default function MobileNav({ activeRoute, navigation, onNavigate }: MobileNavProps) {
  return (
    <nav className="mobile-nav" aria-label="Mobile navigation">
      {navigation.map((item) => (
        <button type="button" key={item.id} aria-current={activeRoute === item.id ? 'page' : undefined} onClick={() => onNavigate(item.id)}>
          <span className="nav-item__icon" aria-hidden="true">{item.shortLabel ?? item.label.slice(0, 2).toUpperCase()}</span>
          <span>{item.label}</span>
        </button>
      ))}
    </nav>
  );
}
