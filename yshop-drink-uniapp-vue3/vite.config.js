import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

export default defineConfig({
  plugins: [uni()],
  server: {
    // 监听所有网卡，便于同一局域网内的手机访问
    host: '0.0.0.0',
    port: 5173,
    strictPort: true,
  },
})
