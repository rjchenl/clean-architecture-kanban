import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vitest/config'

// 流程分析畫面自己的元件測試；開發伺服器與打包在前端的 Main（main/web-ui）
export default defineConfig({
  plugins: [vue()],
  test: {
    environment: 'jsdom',
  },
})
