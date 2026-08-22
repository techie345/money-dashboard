import { describe, expect, it } from 'vitest';
import { parseCsv } from './csv';
import { applyImportRules, normalizeImport } from './normalize';
import { getProfile, profiles } from './profiles';

describe('CSV normalization', () => {
  it('normalizes Chase and SoFi checking debit-credit columns to positive cents and direction', () => {
    const chase = normalizeImport(parseCsv('Details,Posting Date,Description,Amount,Type,Check or Slip #\n,08/21/2026,Coffee Shop,-12.34,DEBIT,').rows, {
      profile: 'chase-checking', accountId: 'chase', sourceFile: 'chase.csv',
    });
    const sofi = normalizeImport(parseCsv('Date,Description,Debit,Credit\n08/22/2026,Paycheck,,1,234.56').rows, {
      profile: 'sofi-bank', accountId: 'sofi', sourceFile: 'sofi.csv',
    });

    expect(chase.transactions[0]).toMatchObject({ amountCents: 1234, direction: 'outflow', kind: 'spending', merchant: 'Coffee Shop' });
    expect(sofi.transactions[0]).toMatchObject({ amountCents: 123456, direction: 'inflow', kind: 'income' });
  });

  it('recovers unquoted comma-separated numeric fields without shifting values', () => {
    const sofi = normalizeImport(parseCsv('Date,Description,Debit,Credit\n08/22/2026,Paycheck,,1,234.56').rows, {
      profile: 'sofi-bank', accountId: 'sofi', sourceFile: 'sofi.csv',
    });
    expect(sofi.errors).toHaveLength(0);
    expect(sofi.transactions[0]).toMatchObject({ amountCents: 123456, direction: 'inflow' });

    const chase = normalizeImport(parseCsv('Posting Date,Description,Debit,Credit\n08/22/2026,Paycheck,,1,234.56').rows, {
      profile: 'chase-checking', accountId: 'chase', sourceFile: 'chase.csv',
    });
    expect(chase.errors).toHaveLength(0);
    expect(chase.transactions[0]).toMatchObject({ amountCents: 123456, direction: 'inflow' });
  });

  it('does not silently interpret malformed numeric overflow as a different amount', () => {
    const result = normalizeImport(parseCsv('Date,Description,Debit,Credit\n08/22/2026,Paycheck,,1,234,oops').rows, {
      profile: 'sofi-bank', accountId: 'sofi', sourceFile: 'sofi.csv',
    });
    expect(result.transactions).toHaveLength(0);
    expect(result.errors[0].message).toMatch(/amount|credit/i);
  });

  it('supports generic field mappings and row corrections', () => {
    const result = normalizeImport(parseCsv('when,who,value\nnot-a-date,Wrong,1\n08/22/2026,Correct,12.34'), {
      profile: 'generic', accountId: 'checking', sourceFile: 'mapped.csv',
      fieldMapping: { date: 'when', description: 'who', amount: 'value' },
      corrections: { 2: { when: '08/21/2026', who: 'Corrected', value: '5.00' } },
    });
    expect(result.errors).toHaveLength(0);
    expect(result.transactions).toMatchObject([
      { date: '2026-08-21', merchant: 'Corrected', amountCents: 500 },
      { date: '2026-08-22', merchant: 'Correct', amountCents: 1234 },
    ]);
  });

  it('revalidates corrected rows using the corrected mapped values', () => {
    const result = normalizeImport(parseCsv('when,who,value\nnot-a-date,Wrong,wat').rows, {
      profile: 'generic', accountId: 'checking', sourceFile: 'mapped.csv',
      fieldMapping: { date: 'when', description: 'who', amount: 'value' },
      corrections: { 2: { date: '08/21/2026', description: 'Corrected', amount: '5.00' } },
    });

    expect(result.errors).toHaveLength(0);
    expect(result.transactions[0]).toMatchObject({ date: '2026-08-21', merchant: 'Corrected', amountCents: 500 });
  });

  it('normalizes generic debit and credit columns without an amount column', () => {
    const result = normalizeImport(parseCsv([
      'Date,Description,Debit,Credit',
      '08/21/2026,Coffee,12.34,',
      '08/22/2026,Paycheck,,1234.56',
    ].join('\n')).rows, {
      profile: 'generic', accountId: 'checking', sourceFile: 'generic.csv',
    });

    expect(result.errors).toHaveLength(0);
    expect(result.transactions).toMatchObject([
      { amountCents: 1234, direction: 'outflow' },
      { amountCents: 123456, direction: 'inflow' },
    ]);
  });

  it('applies corrections to canonical fields through their mapped source columns', () => {
    const result = normalizeImport(parseCsv('when,who,value\nnot-a-date,Wrong,1').rows, {
      profile: 'generic', accountId: 'checking', sourceFile: 'mapped.csv',
      fieldMapping: { date: 'when', description: 'who', amount: 'value' },
      corrections: { 2: { date: '08/21/2026', description: 'Corrected', amount: '5.00' } },
    });
    expect(result.errors).toHaveLength(0);
    expect(result.transactions[0]).toMatchObject({ date: '2026-08-21', merchant: 'Corrected', amountCents: 500 });
  });

  it('normalizes Discover and Amex charges as spending', () => {
    const discover = normalizeImport(parseCsv('Trans. Date,Post Date,Description,Amount,Category\n08/10/2026,08/11/2026,WHOLE FOODS,45.67,Merchandise').rows, {
      profile: 'discover-card', accountId: 'discover', sourceFile: 'discover.csv',
    });
    const amex = normalizeImport(parseCsv('Date,Description,Amount\n08/12/2026,AMAZON.COM,-19.99').rows, {
      profile: 'amex-card', accountId: 'amex', sourceFile: 'amex.csv',
    });

    expect(discover.transactions[0]).toMatchObject({ date: '2026-08-10', amountCents: 4567, kind: 'spending', direction: 'outflow' });
    expect(amex.transactions[0]).toMatchObject({ date: '2026-08-12', amountCents: 1999, kind: 'spending', direction: 'outflow' });
  });

  it('honors card sign conventions and classifies credits, refunds, and payments', () => {
    const result = normalizeImport(parseCsv([
      'Date,Description,Amount',
      '08/12/2026,AMEX PURCHASE,-19.99',
      '08/13/2026,AMEX REFUND,-5.00',
      '08/14/2026,CREDIT CARD PAYMENT,-100.00',
    ].join('\n')).rows, { profile: 'amex-card', accountId: 'amex', sourceFile: 'amex.csv' });

    expect(result.transactions).toHaveLength(3);
    expect(result.transactions).toMatchObject([
      { amountCents: 1999, direction: 'outflow', kind: 'spending' },
      { amountCents: 500, direction: 'inflow', kind: 'refund' },
      { amountCents: 10000, direction: 'inflow', kind: 'credit_card_payment' },
    ]);
  });

  it('only treats explicit refund language as a refund', () => {
    const result = normalizeImport(parseCsv([
      'Date,Description,Amount',
      '08/12/2026,CREDIT CARD PURCHASE,-19.99',
      '08/13/2026,CASHBACK REWARD,-5.00',
      '08/14/2026,MERCHANT REFUND,-5.00',
    ].join('\n')).rows, { profile: 'amex-card', accountId: 'amex', sourceFile: 'amex.csv' });
    expect(result.transactions.map((transaction) => transaction.kind)).toEqual(['spending', 'spending', 'refund']);
  });

  it('reports missing and invalid required fields instead of creating zero-valued records', () => {
    const result = normalizeImport(parseCsv([
      'Date,Description,Amount',
      ',Missing date,12.00',
      '08/13/2026,Missing amount,',
      '08/14/2026,Invalid amount,12oops',
    ].join('\n')).rows, { profile: 'generic', accountId: 'checking', sourceFile: 'invalid.csv' });

    expect(result.transactions).toHaveLength(0);
    expect(result.errors).toHaveLength(3);
  });

  it('parses thousands separators without truncating amounts', () => {
    const result = normalizeImport(parseCsv('Date,Description,Amount\n08/12/2026,Salary,"1,234.56"').rows, {
      profile: 'generic', accountId: 'checking', sourceFile: 'amounts.csv',
    });

    expect(result.transactions[0].amountCents).toBe(123456);
  });

  it('keeps Fidelity activity as investment activity and positions separate', () => {
    const activity = normalizeImport(parseCsv('Run Date,Action,Symbol,Description,Quantity,Price,Amount\n08/01/2026,YOU BOUGHT,VTI,Vanguard Total Stock,2,250.00,-500.00').rows, {
      profile: 'fidelity-activity', accountId: 'fidelity', sourceFile: 'fidelity.csv',
    });
    const positions = normalizeImport(parseCsv('Account,Symbol,Description,Quantity,Last Price,Current Value,Cost Basis Total,% of Account\nBrokerage,VTI,Vanguard Total Stock,2,250.00,500.00,400.00,12.5').rows, {
      profile: 'fidelity-positions', accountId: 'fidelity', sourceFile: 'positions.csv',
    });

    expect(activity.transactions[0]).toMatchObject({ kind: 'investment', category: 'transfer', amountCents: 50000, action: 'YOU BOUGHT', symbol: 'VTI', shares: 2, priceCents: 25000 });
    expect(positions.transactions).toHaveLength(0);
    expect(positions.positions[0]).toMatchObject({ symbol: 'VTI', shares: 2, marketValueCents: 50000, costBasisCents: 40000, allocation: 12.5 });
  });

  it('keeps importedAt and source metadata on non-transaction records', () => {
    const importedAt = '2026-08-21T12:00:00.000Z';
    const positions = normalizeImport(parseCsv('Symbol,Quantity,Current Value\nVTI,2,500.00').rows, {
      profile: 'fidelity-positions', accountId: 'fidelity', sourceFile: 'positions.csv', importedAt,
    });
    const balances = normalizeImport(parseCsv('Date,Balance\n08/01/2026,12,345.67').rows, {
      profile: 'mazda-loan', accountId: 'mazda', sourceFile: 'loan.csv', importedAt,
    });
    expect(positions.positions[0]).toMatchObject({ source: 'Fidelity', sourceFile: 'positions.csv', importedAt });
    expect(balances.balances[0]).toMatchObject({ source: 'Mazda Bank', sourceFile: 'loan.csv', importedAt });
  });

  it('rejects negative shares and allocations over 100 percent', () => {
    const negative = normalizeImport(parseCsv('Symbol,Quantity,Current Value\nVTI,-2,500.00').rows, {
      profile: 'fidelity-positions', accountId: 'fidelity', sourceFile: 'positions.csv',
    });
    const over = normalizeImport(parseCsv('Symbol,Quantity,Current Value,% of Account\nVTI,2,500.00,100.01').rows, {
      profile: 'fidelity-positions', accountId: 'fidelity', sourceFile: 'positions.csv',
    });
    expect(negative.positions).toHaveLength(0);
    expect(over.positions).toHaveLength(0);
  });

  it('rejects invalid Fidelity position fields and loan balances', () => {
    const positions = normalizeImport(parseCsv('Symbol,Quantity,Current Value,Cost Basis Total\nVTI,,500.00,400.00').rows, {
      profile: 'fidelity-positions', accountId: 'fidelity', sourceFile: 'positions.csv',
    });
    const balances = normalizeImport(parseCsv('Date,Description,Balance\n08/01/2026,Loan,').rows, {
      profile: 'mazda-loan', accountId: 'mazda', sourceFile: 'loan.csv',
    });

    expect(positions.positions).toHaveLength(0);
    expect(positions.errors).toHaveLength(1);
    expect(balances.balances).toHaveLength(0);
    expect(balances.errors).toHaveLength(1);
  });

  it('keeps Mazda and Newrez balance rows separate from spending', () => {
    const mazda = normalizeImport(parseCsv('Date,Description,Principal,Interest,Payment,Balance\n08/01/2026,Auto loan payment,300.00,50.00,350.00,12,345.67').rows, {
      profile: 'mazda-loan', accountId: 'mazda', sourceFile: 'mazda.csv',
    });
    const newrez = normalizeImport(parseCsv('Date,Description,Principal,Interest,Payment,Unpaid Principal Balance\n08/01/2026,Mortgage payment,800.00,1200.00,2000.00,250,000.00').rows, {
      profile: 'newrez-mortgage', accountId: 'newrez', sourceFile: 'newrez.csv',
    });

    expect(mazda.transactions).toHaveLength(0);
    expect(mazda.balances[0]).toMatchObject({ balanceCents: 1234567, kind: 'auto_loan' });
    expect(newrez.transactions).toHaveLength(0);
    expect(newrez.balances[0]).toMatchObject({ balanceCents: 25000000, kind: 'mortgage' });
  });

  it('uses a generic fallback, detects normalized headers, and supports manual profile selection', () => {
    expect(getProfile([' DATE ', 'description', 'AMOUNT']).id).toBe('generic');
    expect(getProfile('amex-card').id).toBe('amex-card');
    expect(getProfile(['unrecognized']).id).toBe('generic');
    expect(profiles.some((profile) => profile.id === 'chase-checking')).toBe(true);
    const result = normalizeImport(parseCsv('Date,Description,Amount\n2026-08-20,Transfer to SoFi,-50.00').rows, {
      profile: 'generic', accountId: 'checking', sourceFile: 'generic.csv',
    });
    expect(result.transactions[0]).toMatchObject({ date: '2026-08-20', merchant: 'Transfer to SoFi', amountCents: 5000, kind: 'transfer', direction: 'outflow' });
  });

  it('omits synthetic source IDs and detects broader transfers and payments', () => {
    const result = normalizeImport(parseCsv([
      'Date,Description,Amount',
      '2026-08-20,ACH DEBIT TO SAVINGS,-50.00',
      '2026-08-21,PAYMENT RECEIVED FROM CHECKING,50.00',
      '2026-08-22,ONLINE BILL PAY CARD,-75.00',
    ].join('\n')).rows, { profile: 'generic', accountId: 'checking', sourceFile: 'generic.csv' });

    expect(result.transactions).toMatchObject([
      { kind: 'transfer', direction: 'outflow' },
      { kind: 'transfer', direction: 'inflow' },
      { kind: 'credit_card_payment', direction: 'outflow' },
    ]);
    expect(result.transactions.every((transaction) => transaction.sourceId === undefined)).toBe(true);
  });

  it('preserves import timestamp and applies rules only at the explicit boundary', () => {
    const normalized = normalizeImport(parseCsv('Date,Description,Amount\n2026-08-20,Whole Foods,10.00').rows, {
      profile: 'generic', accountId: 'checking', sourceFile: 'rules.csv', importedAt: '2026-08-21T12:00:00.000Z',
    });
    const result = applyImportRules(normalized, [{ id: 'food', pattern: 'whole foods', category: 'food' }]);

    expect(result.importedAt).toBe('2026-08-21T12:00:00.000Z');
    expect(result.transactions[0]).toMatchObject({ category: 'food', categorySource: 'rule' });
  });

  it('returns row-level errors without discarding valid rows and uses review metadata', () => {
    const result = normalizeImport(parseCsv('Date,Description,Amount\n08/20/2026,Valid,10.00\nnot-a-date,Broken,wat').rows, {
      profile: 'generic', accountId: 'checking', sourceFile: 'bad.csv',
    });
    expect(result.transactions).toHaveLength(1);
    expect(result.transactions[0]).toMatchObject({ reviewStatus: 'needs_review', source: 'Generic CSV', sourceFile: 'bad.csv' });
    expect(result.transactions[0].sourceId).toBeUndefined();
    expect(result.errors[0]).toMatchObject({ row: 3 });
  });

  it('leaves saved merchant rules for the explicit import boundary', () => {
    const result = normalizeImport(parseCsv('Date,Description,Amount\n2026-08-20,Whole Foods,10.00').rows, {
      profile: 'generic', accountId: 'checking', sourceFile: 'rules.csv',
    });
    expect(result.transactions[0].categorySource).toBe('imported');
  });
});
