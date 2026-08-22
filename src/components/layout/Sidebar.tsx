import type { RouteId, NavigationItem } from './AppShell';

export interface SidebarProps {
  activeRoute: RouteId;
  navigation: readonly NavigationItem[];
  onNavigate: (route: RouteId) => void;
}

export default function Sidebar({ activeRoute, navigation, onNavigate }: SidebarProps) {
  return (
    <aside className="sidebar" aria-label="Primary navigation">
      <div className="sidebar__inner">
        <a className="brand" href="#overview" onClick={(event) => { event.preventDefault(); onNavigate('overview'); }}>
          <span className="brand__mark" aria-hidden="true">IM</span>
          <span><strong>Interactive</strong><small>Moneys</small></span>
        </a>
        <p className="sidebar__label">Command center</p>
        <nav>
          {navigation.map((item) => (
            <button
              className={`nav-item${activeRoute === item.id ? ' nav-item--active' : ''}`}
              type="button"
              key={item.id}
              aria-current={activeRoute === item.id ? 'page' : undefined}
              onClick={() => onNavigate(item.id)}
            >
              <span className="nav-item__icon" aria-hidden="true">{item.shortLabel ?? item.label.slice(0, 2).toUpperCase()}</span>
              <span>{item.label}</span>
            </button>
          ))}
        </nav>
        <div className="sidebar__footer">Local-first finance<br /><span>Your data stays here</span></div>
      </div>
    </aside>
  );
}
