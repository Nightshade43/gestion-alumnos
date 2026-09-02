import React from 'react';

type Tone = 'ok' | 'warn' | 'err';
export interface Toast { id: number; tone: Tone; title: string; text?: string; action?: () => void; actionLabel?: string }

const TONE: Record<Tone, { fg: string; bg: string; glyph: string }> = {
  ok:   { fg: '#2F7A4E', bg: '#E2F1E7', glyph: '✓' },
  warn: { fg: '#8F6414', bg: '#FBEFD8', glyph: '!' },
  err:  { fg: '#9E3A38', bg: '#F8E3E1', glyph: '×' },
};

const Ctx = React.createContext<{ push: (t: Omit<Toast, 'id'>) => void }>({ push: () => {} });
export const useToasts = () => React.useContext(Ctx);

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [items, setItems] = React.useState<Toast[]>([]);
  const push = React.useCallback((t: Omit<Toast, 'id'>) => {
    const id = Date.now() + Math.random();
    setItems((xs) => [...xs, { ...t, id }]);
    // los 409/422 quedan hasta que el usuario los descarte; ok/500 se van solos
    if (t.tone === 'ok') setTimeout(() => setItems((xs) => xs.filter((x) => x.id !== id)), 4000);
  }, []);

  return (
    <Ctx.Provider value={{ push }}>
      {children}
      <div style={{ position: 'fixed', right: 26, bottom: 26, zIndex: 70, display: 'flex', flexDirection: 'column', gap: 12 }}>
        {items.map((t) => {
          const tone = TONE[t.tone];
          return (
            <div key={t.id} style={{
              display: 'flex', alignItems: 'flex-start', gap: 12, background: '#fff',
              border: '1px solid var(--ga-line)', borderLeft: `3px solid ${tone.fg}`,
              borderRadius: 'var(--ga-radius-md)', padding: '14px 16px',
              boxShadow: '0 10px 26px rgba(43,39,36,.13)', maxWidth: 380,
              animation: 'ga-toast-in .18s ease-out',
            }}>
              <div style={{ width: 20, height: 20, borderRadius: 999, flex: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 12, fontWeight: 700, background: tone.bg, color: tone.fg }}>{tone.glyph}</div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                <span style={{ fontSize: 14, fontWeight: 600 }}>{t.title}</span>
                {t.text && <span style={{ fontSize: 13, color: 'var(--ga-muted)', lineHeight: 1.45 }}>{t.text}</span>}
                {t.action && (
                  <button onClick={t.action} style={{ marginTop: 6, alignSelf: 'flex-start', background: 'none', border: 'none', padding: 0, fontFamily: 'inherit', fontSize: 13, fontWeight: 600, color: 'var(--ga-primary-600)', cursor: 'pointer' }}>
                    {t.actionLabel ?? 'Resolver'}
                  </button>
                )}
              </div>
              <span onClick={() => setItems((xs) => xs.filter((x) => x.id !== t.id))} style={{ marginLeft: 14, fontSize: 16, color: 'var(--ga-soft)', cursor: 'pointer', lineHeight: 1 }}>×</span>
            </div>
          );
        })}
      </div>
    </Ctx.Provider>
  );
}
