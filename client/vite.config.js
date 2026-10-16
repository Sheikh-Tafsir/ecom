import path from "path"
import react from "@vitejs/plugin-react"
import { defineConfig } from "vite"
import { qrcode } from 'vite-plugin-qrcode';

export default defineConfig(({ mode }) => ({
  plugins: [react(), qrcode()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  esbuild: {
    drop: mode === 'production' ? ['console', 'debugger'] : [],
  },
}))
