import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiTarget = env.VITE_APP_API_URL || 'http://localhost:8800/'

  return {
    plugins: [react(), tailwindcss()],
    resolve: {
      alias: {
        '@': new URL('./src', import.meta.url).pathname
      }
    },
    server: {
      host: '0.0.0.0',
      port: Number(env.VITE_APP_PORT) || 3001,
      open: false,
      proxy: {
        '/api/ws': {
          target: apiTarget,
          ws: true,
          changeOrigin: false,
          rewrite: path => path.replace(/^\/api/, '')
        },
        '/api': {
          target: apiTarget,
          changeOrigin: true,
          rewrite: path => path.replace(/^\/api/, '')
        }
      }
    },
    preview: {
      port: Number(env.VITE_APP_PREVIEW_PORT) || 4301
    },
    build: {
      outDir: 'dist',
      sourcemap: false,
      rollupOptions: {
        output: {
          manualChunks: {
            antdCore: ['antd/es/app', 'antd/es/config-provider', 'antd/locale/zh_CN'],
            router: ['react-router-dom'],
            query: ['@tanstack/react-query'],
            motion: ['framer-motion']
          }
        }
      }
    }
  }
})
