import { fileURLToPath } from 'node:url'
import { resolve } from 'node:path'
import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

const environmentDirectory = resolve(fileURLToPath(new URL('../', import.meta.url)))
const environmentKeys = ['API_PROXY_TARGET', 'LOCAL_API_URL', 'LOCAL_FRONTEND_URL', 'FRONTEND_URL', 'FRONTEND_PORT', 'API_PORT']

export default defineConfig(({ mode }) => {
  const environment = {
    ...loadEnv(mode, environmentDirectory, environmentKeys),
    ...process.env,
  }
  const frontendPort = Number(environment.FRONTEND_PORT || 3000)
  const frontendUrl = environment.FRONTEND_URL
    || environment.LOCAL_FRONTEND_URL
    || `http://localhost:${frontendPort}`
  const apiTarget = environment.API_PROXY_TARGET
    || environment.LOCAL_API_URL
    || `http://localhost:${environment.API_PORT || 8080}`

  return {
    plugins: [react()],
    cacheDir: '.vite',
    server: {
      port: frontendPort,
      allowedHosts: [new URL(frontendUrl).hostname],
      proxy: Object.fromEntries(
        ['/api', '/oauth2', '/login/oauth2'].map((path) => [
          path,
          { target: apiTarget, changeOrigin: true },
        ]),
      ),
    },
  }
})
