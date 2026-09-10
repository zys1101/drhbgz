<template>
  <div class="dashboard">
    <section class="hero">
      <div class="hero-text">
        <h1>欢迎回来，{{ username }} 👋</h1>
        <p>这里是空气质量监测系统的总览看板，随时掌握反馈处理进展与 AQI 级别定义。</p>
        <div class="hero-actions">
          <button class="btn btn-primary" @click="$router.push('/admin/feedback')">📝 前往反馈管理</button>
          <button class="btn btn-ghost" @click="$router.push('/aqi')">📊 查看级别定义</button>
        </div>
      </div>
      <div class="hero-badge">
        <span class="hero-badge-icon">🌍</span>
        <span>守护每一口<br>新鲜空气</span>
      </div>
    </section>

    <section class="stat-grid">
      <div class="stat-card">
        <div class="stat-icon" style="background: #e8f7f0; color: #42b983;">📝</div>
        <div class="stat-info">
          <span class="stat-value">{{ statText(feedbackTotal) }}</span>
          <span class="stat-label">反馈总数</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #fdf6ec; color: #e6a23c;">⏳</div>
        <div class="stat-info">
          <span class="stat-value">{{ statText(pendingTotal) }}</span>
          <span class="stat-label">待处理反馈</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #ecf5ff; color: #409eff;">✅</div>
        <div class="stat-info">
          <span class="stat-value">{{ statText(doneTotal) }}</span>
          <span class="stat-label">已处理反馈</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #f4f0ff; color: #764ba2;">📊</div>
        <div class="stat-info">
          <span class="stat-value">{{ statText(aqiTotal) }}</span>
          <span class="stat-label">已定义级别</span>
        </div>
      </div>
    </section>

    <section class="panel-grid">
      <div class="panel">
        <div class="panel-header">
          <h2>AQI 级别图例</h2>
          <router-link to="/aqi" class="panel-more">管理 →</router-link>
        </div>
        <div v-if="aqiLevels.length" class="legend-list">
          <div v-for="item in aqiLevels" :key="item.aqiId" class="legend-item">
            <span class="legend-color" :style="{ background: item.color }"></span>
            <span class="legend-name">{{ item.chineseExplain }}</span>
            <span class="legend-desc">{{ item.aqiExplain }}</span>
          </div>
        </div>
        <div v-else class="panel-empty">{{ backendTip }}</div>
      </div>

      <div class="panel">
        <div class="panel-header">
          <h2>最新反馈</h2>
          <router-link to="/admin/feedback" class="panel-more">更多 →</router-link>
        </div>
        <div v-if="recentFeedback.length" class="feed-list">
          <div v-for="item in recentFeedback" :key="item.afId" class="feed-item">
            <div class="feed-main">
              <span class="feed-title">{{ item.provinceName || '省份' + item.provinceId }} · {{ item.cityName || '城市' + item.cityId }}</span>
              <span class="feed-sub">{{ item.afDate }} {{ item.afTime }}</span>
            </div>
            <span class="feed-tag" :class="'grade-' + item.estimatedGrade">{{ gradeText(item.estimatedGrade) }}</span>
          </div>
        </div>
        <div v-else class="panel-empty">{{ backendTip }}</div>
      </div>
    </section>
  </div>
</template>

<script>
import { getAqiFeedbackList } from '../api/aqiFeedback.js'
import { getAqiList } from '../api/aqi.js'

export default {
  name: 'HomeView',
  data() {
    return {
      feedbackTotal: null,
      pendingTotal: null,
      doneTotal: null,
      aqiTotal: null,
      aqiLevels: [],
      recentFeedback: []
    }
  },
  computed: {
    username() {
      return this.$store.state.user ? this.$store.state.user.username : ''
    },
    backendTip() {
      return this.feedbackTotal === null && this.aqiTotal === null
        ? '暂无数据（后端服务未连接时会显示此提示）'
        : '暂无数据'
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    statText(v) {
      return v === null ? '—' : v
    },
    gradeText(grade) {
      const map = {
        0: '未评级', 1: '一级', 2: '二级', 3: '三级',
        4: '四级', 5: '五级', 6: '六级'
      }
      return map[grade] || '未知'
    },
    async loadData() {
      try {
        const [fbRes, aqiRes] = await Promise.allSettled([getAqiFeedbackList(), getAqiList()])
        if (fbRes.status === 'fulfilled' && fbRes.value.data.code === 200) {
          const list = fbRes.value.data.data || []
          this.feedbackTotal = list.length
          this.pendingTotal = list.filter(i => i.state !== 3).length
          this.doneTotal = list.filter(i => i.state === 3).length
          this.recentFeedback = list.slice(0, 6)
        }
        if (aqiRes.status === 'fulfilled' && aqiRes.value.data.code === 200) {
          const list = aqiRes.value.data.data || []
          this.aqiTotal = list.length
          this.aqiLevels = list
        }
      } catch (err) {
        // 后端未启动时静默降级，页面显示占位文案
        console.warn('看板数据加载失败（后端可能未启动）', err)
      }
    }
  }
}
</script>

<style scoped>
.dashboard {
  padding: 28px 32px;
  text-align: left;
}

/* ---------- 欢迎横幅 ---------- */
.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  background: linear-gradient(135deg, #42b983 0%, #2c3e50 100%);
  border-radius: 16px;
  padding: 34px 38px;
  color: #fff;
  margin-bottom: 24px;
  box-shadow: 0 12px 32px rgba(44, 62, 80, 0.25);
}

.hero-text h1 {
  margin: 0 0 8px;
  font-size: 24px;
}

.hero-text p {
  margin: 0 0 20px;
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
}

.hero-actions {
  display: flex;
  gap: 12px;
}

.hero-badge {
  display: flex;
  align-items: center;
  gap: 12px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 14px;
  padding: 16px 20px;
  font-size: 14px;
  line-height: 1.5;
  white-space: nowrap;
}

.hero-badge-icon {
  font-size: 34px;
}

/* ---------- 按钮 ---------- */
.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary {
  background: #fff;
  color: #2c7a5a;
}

.btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
}

.btn-ghost {
  background: rgba(255, 255, 255, 0.14);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.35);
}

.btn-ghost:hover {
  background: rgba(255, 255, 255, 0.24);
}

/* ---------- 统计卡片 ---------- */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
  margin-bottom: 24px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: #2c3e50;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #909399;
}

/* ---------- 面板 ---------- */
.panel-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}

.panel {
  background: #fff;
  border-radius: 12px;
  padding: 22px 24px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.panel-header h2 {
  margin: 0;
  font-size: 16px;
  color: #2c3e50;
}

.panel-more {
  font-size: 13px;
  color: #42b983;
  text-decoration: none;
}

.panel-more:hover {
  text-decoration: underline;
}

.panel-empty {
  padding: 40px 0;
  text-align: center;
  color: #c0c4cc;
  font-size: 14px;
}

/* 级别图例 */
.legend-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 9px 12px;
  border-radius: 8px;
  background: #fafbfc;
}

.legend-color {
  width: 18px;
  height: 18px;
  border-radius: 5px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  flex-shrink: 0;
}

.legend-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  min-width: 48px;
}

.legend-desc {
  font-size: 13px;
  color: #909399;
}

/* 最新反馈 */
.feed-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.feed-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #fafbfc;
}

.feed-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.feed-title {
  font-size: 14px;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.feed-sub {
  font-size: 12px;
  color: #a8abb2;
}

.feed-tag {
  flex-shrink: 0;
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 12px;
  background: #e1f3d8;
  color: #67c23a;
}

.feed-tag.grade-0 { background: #f4f4f5; color: #909399; }
.feed-tag.grade-4 { background: #fdf6ec; color: #e6a23c; }
.feed-tag.grade-5,
.feed-tag.grade-6 { background: #fef0f0; color: #f56c6c; }

@media (max-width: 1100px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .panel-grid {
    grid-template-columns: 1fr;
  }
}
</style>
