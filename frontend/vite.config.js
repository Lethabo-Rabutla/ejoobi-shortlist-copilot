import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// Any request starting with /api is passed on to the java backend.
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
