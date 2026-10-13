import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In development, every request to /api is forwarded to the Spring Boot
// backend on port 8080. The browser only ever talks to the Vite server,
// so there are no CORS problems and the code can just call "/api/...".
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
