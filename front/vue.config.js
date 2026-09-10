const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true,
  devServer: {
    host: '0.0.0.0',      // 允许容器/远程预览访问
    port: 8080,
    allowedHosts: 'all',
    // 禁用缓存：确保浏览器/预览代理始终拿到最新编译产物
    headers: {
      'Cache-Control': 'no-store, must-revalidate'
    },
    // 将 /api 请求代理到后端服务（SpringBoot: http://localhost:9000，上下文 /api）
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:9000',
        changeOrigin: true
      }
    }
  }
})
