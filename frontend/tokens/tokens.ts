// Espejo tipado de tokens.css — usar cuando haga falta un valor en JS/TS.
export const color = {
  primary50: '#F4F1FA', primary100: '#E6E0F1', primary400: '#75609F',
  primary600: '#5D4A87', primary700: '#4A3A6E',
  canvas: '#FAF7F2', surface: '#FFFFFF', surface2: '#FAF7F2', surface3: '#F4EFE8',
  line: '#E6DED2', lineSoft: '#F0EAE1', lineStrong: '#D6CBBB',
  ink: '#2B2724', inkSoft: '#3E3833', muted: '#6F675E', soft: '#948B80',
  navBg: '#2B2724', navHover: '#3A3531', navText: '#D3CBC1', navLabel: '#7C736A',
  escolar: '#3E6C8A', particular: '#A8762B',
} as const;

export const statusTone = {
  ACTIVA:     { fg: '#2F7A4E', bg: '#E2F1E7', line: '#C3E2CE' },
  PAUSADA:    { fg: '#8F6414', bg: '#FBEFD8', line: '#EEDCB4' },
  FINALIZADA: { fg: '#3F6183', bg: '#E4EDF4', line: '#C9DCEA' },
  CANCELADA:  { fg: '#9E3A38', bg: '#F8E3E1', line: '#EDC9C6' },
  ACTIVO:     { fg: '#2F7A4E', bg: '#E2F1E7', line: '#C3E2CE' },
  FINALIZADO: { fg: '#3F6183', bg: '#E4EDF4', line: '#C9DCEA' },
} as const;

export const radius = { xs: 6, sm: 9, md: 12, lg: 14, xl: 16, pill: 999 } as const;
export const space = { 1: 4, 2: 8, 3: 10, 4: 14, 5: 16, 6: 20, 7: 22, 8: 24, 9: 34 } as const;
export const font = {
  ui: "'Instrument Sans', system-ui, sans-serif",
  mono: "'IBM Plex Mono', ui-monospace, monospace",
} as const;
