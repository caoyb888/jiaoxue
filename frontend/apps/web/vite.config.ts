import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
      '@edu/ui': path.resolve(__dirname, '../../packages/ui/src'),
      '@edu/api': path.resolve(__dirname, '../../packages/api/src'),
      '@edu/store': path.resolve(__dirname, '../../packages/store/src'),
      '@edu/utils': path.resolve(__dirname, '../../packages/utils/src'),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://100.84.68.115:18080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'ws://100.84.68.115:8090',
        ws: true,
        changeOrigin: true,
      },
    },
  },
})
