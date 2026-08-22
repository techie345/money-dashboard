import Papa from 'papaparse';

export interface CsvRow {
  row: number;
  data: Record<string, string>;
}

export interface CsvParseError {
  row: number;
  message: string;
}

export interface ParsedCsv {
  headers: string[];
  rows: CsvRow[];
  errors: CsvParseError[];
}

export function normalizeHeader(header: string): string {
  return header.trim().toLowerCase().replace(/[^a-z0-9]+/g, ' ').trim();
}

export function parseCsv(input: string): ParsedCsv {
  const errors: CsvParseError[] = [];
  const parsed = Papa.parse<Record<string, string>>(input, {
    header: true,
    skipEmptyLines: 'greedy',
  });

  for (const error of parsed.errors) errors.push({ row: (error.row ?? 0) + 2, message: error.message });
  const headers = parsed.meta.fields ?? [];
  const rows = parsed.data.map((data, index) => ({ row: index + 2, data }));
  return { headers, rows, errors };
}
