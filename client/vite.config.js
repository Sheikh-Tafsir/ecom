import path from "path"
import react from "@vitejs/plugin-react"
import { defineConfig } from "vite"
import { qrcode } from 'vite-plugin-qrcode';

export default defineConfig(({ mode }) => ({
  plugins: [react(), qrcode()],
  server: {
    host: true, // binds to 0.0.0.0 — accessible from other devices on LAN
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/socket.io': {
        target: 'http://localhost:8080',
        ws: true,
      },
    },
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  esbuild: {
    drop: mode === 'production' ? ['console', 'debugger'] : [],
  },
}))
