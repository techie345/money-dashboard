import { useState } from 'react';
import type { ChangeEvent } from 'react';
import type { FinancialDataset } from '../../domain/types';
import { getMerchantRules, saveDataset } from '../../data/storage';
import { parseCsv, type ParsedCsv } from '../../import/csv';
import { applyImportRules, normalizeImport, type FieldMapping, type ImportField, type ImportCorrections, type NormalizeResult } from '../../import/normalize';
import { detectProfile, getProfile, profiles, type ProfileId } from '../../import/profiles';
import { findDuplicateCandidates, findIncomingDuplicateIds } from '../../import/dedupe';
import { createImportedDataset } from '../../import/commit';

export interface ImportPageProps {
  dataset: FinancialDataset;
  onDatasetChange: (dataset: FinancialDataset) => void;
}

const fields: ImportField[] = ['date', 'description', 'amount', 'debit', 'credit'];
const emptyParsed: ParsedCsv = { headers: [], rows: [], errors: [] };
const timestamp = () => new Date().toISOString();
const displayTimestamp = (value?: string) => value ? new Date(value).toLocaleString() : 'Not imported yet';

export default function ImportPage({ dataset, onDatasetChange }: ImportPageProps) {
  const [parsed, setParsed] = useState<ParsedCsv>();
  const [fileName, setFileName] = useState('');
  const [importedAt, setImportedAt] = useState('');
  const [profileId, setProfileId] = useState<ProfileId>('generic');
  const [accountId, setAccountId] = useState(dataset.accounts[0]?.id ?? '');
  const [mapping, setMapping] = useState<FieldMapping>({});
  const [corrections, setCorrections] = useState<ImportCorrections>({});
  const [message, setMessage] = useState('');

  const profile = getProfile(profileId);
  const rules = getMerchantRules();
  const normalized = parsed ? applyImportRules(normalizeImport(parsed, {
    profile,
    accountId,
    sourceFile: fileName,
    importedAt,
    fieldMapping: mapping,
    corrections,
  }), rules.length ? rules : dataset.merchantRules) : undefined;
  const duplicates = normalized ? findDuplicateCandidates(normalized.transactions, dataset.transactions) : [];
  const duplicateIds = new Set([...duplicates.map((candidate) => candidate.incomingId), ...(normalized ? findIncomingDuplicateIds(normalized.transactions) : [])]);
  const canCommit = Boolean(normalized && accountId && normalized.errors.length === 0);

  async function handleUpload(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) return;
    try {
      const text = await file.text();
      const nextParsed = parseCsv(text);
      setParsed(nextParsed);
      setFileName(file.name);
      setImportedAt(timestamp());
      setProfileId(detectProfile(nextParsed.headers).id);
      setMapping({});
      setCorrections({});
      setMessage('');
    } catch (error) {
      setMessage(`Could not read file: ${error instanceof Error ? error.message : String(error)}`);
    }
  }

  async function commitImport() {
    if (!normalized || !canCommit) return;
    try {
      const incomingTransactions = normalized.transactions.filter((transaction) => !duplicateIds.has(transaction.id));
      const next: FinancialDataset = { ...createImportedDataset(dataset, normalized, incomingTransactions), merchantRules: rules.length ? rules : dataset.merchantRules };
      await saveDataset(next);
      onDatasetChange(next);
      setMessage(`Imported ${incomingTransactions.length} records. ${duplicateIds.size} duplicate${duplicateIds.size === 1 ? '' : 's'} skipped.`);
    } catch (error) {
      setMessage(`Could not save import: ${error instanceof Error ? error.message : String(error)}`);
    }
  }

  const updateMapping = (field: ImportField, value: string) => setMapping((current) => ({ ...current, [field]: value || undefined }));
  const updateCorrection = (row: number, field: string, value: string) => setCorrections((current) => ({ ...current, [row]: { ...current[row], [field]: value } }));

  return <div className="page-stack">
    <header className="page-intro"><div><p className="page-kicker">Local data pipeline</p><h1>Import</h1><p className="page-subtitle">Upload, validate, review, then commit. Nothing changes until confirmation.</p></div></header>
    <section className="signal-panel" aria-labelledby="upload-heading">
      <div className="section-header"><div><p className="section-header__eyebrow">Step 01</p><h2 id="upload-heading">Upload a CSV</h2></div></div>
      <label className="file-picker">Choose CSV file<input type="file" accept=".csv,text/csv" onChange={handleUpload} /></label>
      <p className="metadata-line">File: <strong>{fileName || 'None selected'}</strong> · Imported: <strong>{displayTimestamp(importedAt)}</strong></p>
    </section>
    <section className="signal-panel" aria-labelledby="source-heading">
      <div className="section-header"><div><p className="section-header__eyebrow">Step 02</p><h2 id="source-heading">Source profile</h2></div></div>
      <div className="form-grid"><label>Profile<select value={profileId} onChange={(event) => setProfileId(event.target.value as ProfileId)}>{profiles.map((item) => <option key={item.id} value={item.id}>{item.institution} · {item.sourceType}</option>)}</select></label><label>Destination account<select value={accountId} onChange={(event) => setAccountId(event.target.value)}>{dataset.accounts.map((account) => <option key={account.id} value={account.id}>{account.institution} · {account.name}</option>)}</select></label></div>
      <p className="metadata-line">Detected: <strong>{profile.institution}</strong> · {profile.signConvention}</p>
    </section>
    <section className="signal-panel" aria-labelledby="mapping-heading">
      <div className="section-header"><div><p className="section-header__eyebrow">Steps 03–04</p><h2 id="mapping-heading">Mapping and validation</h2><p className="section-header__description">Correct generic columns or fix values before committing.</p></div></div>
      {parsed ? <>{profile.id === 'generic' && <div className="form-grid mapping-grid">{fields.map((field) => <label key={field}>{field}<select value={mapping[field] ?? ''} onChange={(event) => updateMapping(field, event.target.value)}><option value="">Auto detect</option>{parsed.headers.map((header) => <option key={header} value={header}>{header}</option>)}</select></label>)}</div>}</> : <p className="metadata-line">Upload a file to preview detected rows and validation results.</p>}
       <ValidationSummary parsed={parsed ?? emptyParsed} normalized={normalized} duplicateCount={duplicateIds.size} />
      {normalized && normalized.errors.length > 0 && <div className="error-list" role="alert"><h3>Parse errors</h3>{normalized.errors.map((error) => <p key={`${error.row}-${error.message}`}>Row {error.row}: {error.message}</p>)}</div>}
       {normalized && normalized.transactions.length > 0 && <div className="table-wrap"><table><caption>Transaction preview</caption><thead><tr><th scope="col">Row</th><th scope="col">Date</th><th scope="col">Description</th><th scope="col">Amount</th><th scope="col">Validation status</th></tr></thead><tbody>{normalized.transactions.slice(0, 20).map((transaction) => <tr key={transaction.id}><td>{transaction.id.split('-').at(-1)}</td><td>{transaction.date}</td><td>{transaction.merchant}</td><td>{(transaction.amountCents / 100).toFixed(2)}</td><td>{duplicateIds.has(transaction.id) ? 'Duplicate' : transaction.reviewStatus === 'needs_review' ? 'Review required' : 'Valid'}</td></tr>)}</tbody></table></div>}
       {normalized?.errors.map((error) => <div className="correction-row" key={`correction-${error.row}`}><strong>Correct row {error.row}</strong>{fields.map((field) => <label key={field}>{field}<input value={corrections[error.row]?.[field] ?? ''} placeholder={`${field} correction`} onChange={(event) => updateCorrection(error.row, field, event.target.value)} /></label>)}</div>)}
      <button className="button" type="button" disabled={!canCommit} onClick={() => void commitImport()}>Confirm and commit import</button>
      {message && <p className="success-message" role="status">{message}</p>}
    </section>
  </div>;
}

function ValidationSummary({ parsed, normalized, duplicateCount }: { parsed: ParsedCsv; normalized?: NormalizeResult; duplicateCount: number }) {
  const counts = [
    ['Detected rows', parsed.rows.length], ['Validation errors', normalized?.errors.length ?? parsed.errors.length],
    ['Duplicate candidates', duplicateCount], ['Transfers', normalized?.transactions.filter((item) => item.kind === 'transfer' || item.kind === 'credit_card_payment').length ?? 0],
    ['Investments', (normalized?.positions.length ?? 0) + (normalized?.transactions.filter((item) => item.kind === 'investment').length ?? 0)],
    ['Uncategorized', normalized?.transactions.filter((item) => item.category === 'other').length ?? 0],
    ['Needs review', normalized?.transactions.filter((item) => item.reviewStatus === 'needs_review').length ?? 0],
  ];
  return <div className="import-summary" aria-label="Validation summary">{counts.map(([label, count]) => <div key={label}><strong>{count}</strong><span>{label}</span></div>)}</div>;
}
