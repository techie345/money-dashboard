export interface PrivacyValueProps {
  value: string;
  visible?: boolean;
  onToggle?: () => void;
  label?: string;
}

export default function PrivacyValue({ value, visible = true, onToggle, label = 'financial value' }: PrivacyValueProps) {
  const content = visible ? value : '••••••';
  if (!onToggle) return <span className="privacy-value">{content}</span>;
  return (
    <button className="privacy-value" type="button" onClick={onToggle} aria-label={`${visible ? 'Hide' : 'Show'} ${label}`}>
      {content}
    </button>
  );
}
