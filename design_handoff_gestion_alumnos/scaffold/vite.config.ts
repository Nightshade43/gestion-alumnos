import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

/**
 * El proxy evita configurar CORS en el backend durante el desarrollo: el front
 * pide /api/... al mismo origen (localhost:5173) y Vite lo reenvía a Spring Boot.
 * client.ts usa BASE = '' por eso mismo.
 *
 * Para el deploy: definir VITE_API_BASE con el origen real del backend y recién
 * ahí configurar CORS en Spring (el proxy no participa del build de producción).
 */
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
