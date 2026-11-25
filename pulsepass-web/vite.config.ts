import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// PRD Frontend §17: React + Tailwind v4. En desarrollo, /api se reenvía al backend (GAP-05: sin CORS).
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: { '/api': 'http://localhost:8080' },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    css: false,
    // QT-FE-009: pruebas deterministas, sin red real (MSW intercepta todo)
  },
})
