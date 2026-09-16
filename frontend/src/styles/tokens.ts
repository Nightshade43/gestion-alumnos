// Espejo tipado de tokens.css — usar cuando haga falta un valor en JS/TS.
export const color = {
  primary50: '#1A1830', primary100: '#241F3F', primary400: '#7A6FD0',
  primary600: '#9D8FF7', primary700: '#BB78F7',
  canvas: '#0D0F18', surface: '#13162A', surface2: '#191D35', surface3: '#1E2240',
  line: '#1E2240', lineSoft: '#191D35', lineStrong: '#2D3260',
  ink: '#DDE1FF', inkSoft: '#C8CCF0', muted: '#8890C0', soft: '#6670A0',
  navBg: '#0A0C16', navHover: '#191D35', navText: '#C8CCF0', navLabel: '#4A5280',
  escolar: '#7DCFB6', particular: '#F0883E',
} as const;

export const statusTone = {
  ACTIVA:     { fg: '#5EC97E', bg: 'rgba(94,201,126,.13)',  line: 'rgba(94,201,126,.30)' },
  PAUSADA:    { fg: '#E8A24A', bg: 'rgba(232,162,74,.14)',  line: 'rgba(232,162,74,.30)' },
  FINALIZADA: { fg: '#7AB4F7', bg: 'rgba(122,180,247,.13)', line: 'rgba(122,180,247,.30)' },
  CANCELADA:  { fg: '#E87A7A', bg: 'rgba(232,122,122,.12)', line: 'rgba(232,122,122,.30)' },
  ACTIVO:     { fg: '#5EC97E', bg: 'rgba(94,201,126,.13)',  line: 'rgba(94,201,126,.30)' },
  FINALIZADO: { fg: '#7AB4F7', bg: 'rgba(122,180,247,.13)', line: 'rgba(122,180,247,.30)' },
} as const;

export const radius = { xs: 6, sm: 9, md: 12, lg: 14, xl: 16, pill: 999 } as const;
export const space = { 1: 4, 2: 8, 3: 10, 4: 14, 5: 16, 6: 20, 7: 22, 8: 24, 9: 34 } as const;
export const font = {
  ui: "'IBM Plex Sans', system-ui, sans-serif",
  mono: "'IBM Plex Mono', ui-monospace, monospace",
} as const;
