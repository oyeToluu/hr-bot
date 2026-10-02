import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In development, requests to /api/* are forwarded to the Spring Boot
// backend on port 8080, so the browser never makes a cross-origin call.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
});
