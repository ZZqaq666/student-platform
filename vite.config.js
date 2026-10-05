import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) {
            return undefined
          }

          if (id.includes('element-plus') || id.includes('@element-plus')) {
            return 'vendor-element'
          }
          if (id.includes('echarts')) {
            return 'vendor-charts'
          }
          if (
            id.includes('katex') ||
            id.includes('markdown-it') ||
            id.includes('highlight.js') ||
            id.includes('dompurify')
          ) {
            return 'vendor-markdown'
          }
          if (id.includes('html2canvas') || id.includes('jspdf')) {
            return 'vendor-pdf'
          }
          if (id.includes('vue') || id.includes('vue-router') || id.includes('pinia')) {
            return 'vendor-vue'
          }

          return 'vendor'
        }
      }
    }
  }
})
