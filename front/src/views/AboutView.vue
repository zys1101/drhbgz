<template>
  <div class="about-page">
    <div class="about-card">
      <div class="about-logo">🌿</div>
      <h1>空气质量监测系统</h1>
      <p class="about-version">Air Quality Monitoring System · v1.0</p>

      <div class="about-desc">
        本系统用于收集和管理公众提交的空气质量反馈，并对 AQI 级别进行统一定义与维护，
        帮助管理者及时了解各地空气状况并做出响应。
      </div>

      <div class="feature-grid">
        <div class="feature-item">
          <span class="feature-icon">📝</span>
          <span class="feature-title">反馈管理</span>
          <span class="feature-desc">新增、修改、删除公众提交的空气质量反馈记录</span>
        </div>
        <div class="feature-item">
          <span class="feature-icon">📊</span>
          <span class="feature-title">级别维护</span>
          <span class="feature-desc">定义 AQI 级别的颜色、污染物浓度限值与健康建议</span>
        </div>
        <div class="feature-item">
          <span class="feature-icon">📈</span>
          <span class="feature-title">数据看板</span>
          <span class="feature-desc">首页汇总反馈处理进展与级别定义情况</span>
        </div>
      </div>

      <div class="about-footer">
        <span>技术栈：Vue 3 · Vue Router · Vuex · Axios</span>
        <span class="dot">·</span>
        <span>Spring Boot 后端</span>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AboutView'
}
</script>

<style scoped>
.about-page {
  padding: 40px 32px;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

.about-card {
  width: 720px;
  max-width: 100%;
  background: #fff;
  border-radius: 16px;
  padding: 44px 48px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  text-align: center;
}

.about-logo {
  font-size: 48px;
  margin-bottom: 10px;
}

h1 {
  margin: 0 0 6px;
  font-size: 24px;
  color: #2c3e50;
}

.about-version {
  margin: 0 0 22px;
  font-size: 13px;
  color: #a8abb2;
  letter-spacing: 0.5px;
}

.about-desc {
  font-size: 14px;
  line-height: 1.8;
  color: #606266;
  background: #f6f9f8;
  border-left: 4px solid #42b983;
  border-radius: 8px;
  padding: 14px 18px;
  text-align: left;
  margin-bottom: 26px;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  margin-bottom: 28px;
}

.feature-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px 14px;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  transition: all 0.2s;
}

.feature-item:hover {
  border-color: #42b983;
  box-shadow: 0 6px 16px rgba(66, 185, 131, 0.15);
  transform: translateY(-2px);
}

.feature-icon {
  font-size: 28px;
}

.feature-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.feature-desc {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}

.about-footer {
  font-size: 12px;
  color: #c0c4cc;
  display: flex;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
}

@media (max-width: 720px) {
  .feature-grid {
    grid-template-columns: 1fr;
  }
  .about-card {
    padding: 32px 24px;
  }
}
</style>
