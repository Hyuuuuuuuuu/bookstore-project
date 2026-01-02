import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // Provide a browser global for libraries that expect Node's `global`
  define: {
    global: 'window'
  },
  server: {
    // Automatically open the browser when the dev server starts
    open: true,
    proxy: {
      '/api': {
        target: 'http://localhost:5000',
        changeOrigin: true,
        secure: false,
      }
    }
  }
})
