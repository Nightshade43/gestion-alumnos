import React from 'react';
import { createRoot } from 'react-dom/client';
import './styles/tokens.css';
import { App } from './App';

/**
 * Único punto de entrada. tokens.css se importa acá y en ningún otro lugar:
 * define las variables, el reset de html/body y los tres keyframes
 * (ga-shimmer, ga-spin, ga-toast-in) que usan las primitivas.
 *
 * El QueryClientProvider y el ToastProvider viven dentro de App.tsx.
 */
createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
