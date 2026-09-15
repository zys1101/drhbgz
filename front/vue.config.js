const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true,
  devServer: {
    host: '0.0.0.0',      // 允许容器/远程预览访问
    port: 8080,
    allowedHosts: 'all',
    compress: false,      // 关闭 gzip 压缩，避免 SSE 流式响应被代理缓冲（否则 AI 回复会一次性全出现）
    // 禁用缓存：确保浏览器/预览代理始终拿到最新编译产物
    headers: {
      'Cache-Control': 'no-store, must-revalidate'
    },
    // 将 /api 请求代理到后端服务（SpringBoot: http://localhost:9000，上下文 /api）
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:9000',
        changeOrigin: true
        // 保持默认透传：http-proxy-middleware 默认就是流式 pipe，不做任何 header 干预
      }
    }
  }
})
