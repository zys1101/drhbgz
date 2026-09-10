<template>
  <div class="page">
    <div class="card">
      <div class="card-header">
        <div>
          <h2 class="card-title">统计数据管理</h2>
          <p class="card-sub">确认后的 AQI 数据自动纳入以下统计维度</p>
        </div>
        <span v-if="mock" class="mock-badge">演示数据（后端未连接）</span>
      </div>

      <div class="tab-bar">
        <button
          v-for="t in tabs"
          :key="t.key"
          class="tab-btn"
          :class="{ active: activeTab === t.key }"
          @click="activeTab = t.key"
        >{{ t.label }}</button>
      </div>

      <!-- 1. 省分组超标统计 -->
      <div v-show="activeTab === 'province'" class="tab-body">
        <VChart :option="provinceOption" height="420px" />
      </div>

      <!-- 2. AQI 指数分布统计 -->
      <div v-show="activeTab === 'distribution'" class="tab-body">
        <VChart :option="distributionOption" height="420px" />
      </div>

      <!-- 3. AQI 指数趋势统计 -->
      <div v-show="activeTab === 'trend'" class="tab-body">
        <VChart :option="trendOption" height="420px" />
      </div>

      <!-- 4. 实时统计 -->
      <div v-show="activeTab === 'realtime'" class="tab-body">
        <div class="rt-grid">
          <div class="rt-card">
            <span class="rt-icon" style="background: #ecf5ff; color: #409eff;">🧪</span>
            <span class="rt-value">{{ realtime.total }}</span>
            <span class="rt-label">AQI检测累计数量</span>
          </div>
          <div class="rt-card">
            <span class="rt-icon" style="background: #f0f9eb; color: #67c23a;">🌿</span>
            <span class="rt-value">{{ realtime.good }}</span>
            <span class="rt-label">检测结果良好累计数量</span>
          </div>
          <div class="rt-card">
            <span class="rt-icon" style="background: #fef0f0; color: #f56c6c;">⚠️</span>
            <span class="rt-value">{{ realtime.exceed }}</span>
            <span class="rt-label">检测结果超标累计数量</span>
          </div>
        </div>
        <VChart :option="realtimePieOption" height="360px" />
      </div>

      <!-- 5. 全国网格覆盖率统计 -->
      <div v-show="activeTab === 'coverage'" class="tab-body">
        <div class="cov-grid">
          <div class="cov-card">
            <div class="cov-num">{{ coverage.provinceCovered }}<span class="cov-total"> / {{ coverage.provinceTotal }}</span></div>
            <div class="cov-name">覆盖省份</div>
            <div class="cov-bar"><div class="cov-inner" :style="{ width: provincePercent + '%' }"></div></div>
            <div class="cov-pct">覆盖率 {{ provincePercent }}%</div>
          </div>
          <div class="cov-card">
            <div class="cov-num">{{ coverage.cityCovered }}<span class="cov-total"> / {{ coverage.cityTotal }}</span></div>
            <div class="cov-name">覆盖大城市（2022年名单）</div>
            <div class="cov-bar"><div class="cov-inner" :style="{ width: cityPercent + '%' }"></div></div>
            <div class="cov-pct">覆盖率 {{ cityPercent }}%</div>
          </div>
        </div>
        <div class="cov-list-title">已覆盖网格区域</div>
        <div class="cov-list">
          <span v-for="(c, i) in coverage.coveredList" :key="i" class="cov-item">
            📍 {{ c.province }} · {{ c.city }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import VChart from '../../components/VChart.vue'
import {
  getProvinceStats, getDistributionStats, getTrendStats,
  getRealtimeStats, getCoverageStats
} from '../../api/stats'

export default {
  name: 'StatsView',
  components: { VChart },
  data() {
    return {
      activeTab: 'province',
      tabs: [
        { key: 'province', label: '省分组超标统计' },
        { key: 'distribution', label: 'AQI指数分布' },
        { key: 'trend', label: 'AQI指数趋势' },
        { key: 'realtime', label: '实时统计' },
        { key: 'coverage', label: '网格覆盖率' }
      ],
      mock: false,
      provinceRows: [],
      distribution: [],
      trend: [],
      realtime: { total: 0, good: 0, exceed: 0 },
      coverage: { provinceCovered: 0, provinceTotal: 34, cityCovered: 0, cityTotal: 106, coveredList: [] }
    }
  },
  computed: {
    provincePercent() {
      const c = this.coverage
      return c.provinceTotal ? Math.round(c.provinceCovered / c.provinceTotal * 100) : 0
    },
    cityPercent() {
      const c = this.coverage
      return c.cityTotal ? Math.round(c.cityCovered / c.cityTotal * 100) : 0
    },
    provinceOption() {
      return {
        color: ['#f56c6c', '#e6a23c', '#409eff', '#2e8565'],
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        legend: { data: ['SO₂超标', 'CO超标', 'PM2.5超标', 'AQI等级超标'], top: 6 },
        grid: { left: 50, right: 24, top: 60, bottom: 70 },
        xAxis: {
          type: 'category',
          data: this.provinceRows.map(r => r.province),
          axisLabel: { rotate: 30 }
        },
        yAxis: { type: 'value', name: '累计数量' },
        series: [
          { name: 'SO₂超标', type: 'bar', data: this.provinceRows.map(r => r.so2), barMaxWidth: 26 },
          { name: 'CO超标', type: 'bar', data: this.provinceRows.map(r => r.co), barMaxWidth: 26 },
          { name: 'PM2.5超标', type: 'bar', data: this.provinceRows.map(r => r.pm25), barMaxWidth: 26 },
          { name: 'AQI等级超标', type: 'bar', data: this.provinceRows.map(r => r.aqi), barMaxWidth: 26 }
        ]
      }
    },
    distributionOption() {
      return {
        color: ['#95e8a7', '#b3e19d', '#f3d19e', '#fab6b6', '#f89898', '#7b4a12'],
        tooltip: { trigger: 'item', formatter: '{b}：{c} 条（{d}%）' },
        legend: { orient: 'vertical', left: 'left', top: 'middle' },
        series: [
          {
            name: 'AQI等级分布',
            type: 'pie',
            radius: ['42%', '68%'],
            center: ['55%', '52%'],
            label: { formatter: '{b}\n{d}%' },
            data: this.distribution
          }
        ]
      }
    },
    trendOption() {
      return {
        color: ['#e6a23c'],
        tooltip: { trigger: 'axis' },
        grid: { left: 50, right: 30, top: 40, bottom: 40 },
        xAxis: { type: 'category', data: this.trend.map(t => t.month), boundaryGap: false },
        yAxis: { type: 'value', name: '全国AQI超标累计数量' },
        series: [
          {
            name: 'AQI超标累计',
            type: 'line',
            data: this.trend.map(t => t.exceed),
            smooth: true,
            areaStyle: { opacity: 0.15 },
            lineStyle: { width: 3 },
            symbolSize: 7
          }
        ]
      }
    },
    realtimePieOption() {
      return {
        color: ['#67c23a', '#f56c6c'],
        tooltip: { trigger: 'item', formatter: '{b}：{c} 条（{d}%）' },
        legend: { bottom: 0 },
        series: [
          {
            name: '检测结果',
            type: 'pie',
            radius: ['42%', '66%'],
            center: ['50%', '46%'],
            label: { formatter: '{b}\n{d}%' },
            data: [
              { name: '良好', value: this.realtime.good },
              { name: '超标', value: this.realtime.exceed }
            ]
          }
        ]
      }
    }
  },
  async created() {
    const [p, d, t, r, c] = await Promise.allSettled([
      getProvinceStats(), getDistributionStats(), getTrendStats(),
      getRealtimeStats(), getCoverageStats()
    ])
    if (p.status === 'fulfilled') { this.provinceRows = p.value.data; this.mock = this.mock || p.value.mock }
    if (d.status === 'fulfilled') { this.distribution = d.value.data; this.mock = this.mock || d.value.mock }
    if (t.status === 'fulfilled') { this.trend = t.value.data; this.mock = this.mock || t.value.mock }
    if (r.status === 'fulfilled') { this.realtime = r.value.data; this.mock = this.mock || r.value.mock }
    if (c.status === 'fulfilled') { this.coverage = c.value.data; this.mock = this.mock || c.value.mock }
  }
}
</script>

<style scoped>
.page {
  padding: 28px 32px;
  text-align: left;
}

.card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.card-header {
  padding: 22px 28px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  margin: 0;
  font-size: 19px;
  color: #2c3e50;
}

.card-sub {
  margin: 5px 0 0;
  font-size: 13px;
  color: #a8abb2;
}

.mock-badge {
  font-size: 12px;
  color: #e6a23c;
  background: #fdf6ec;
  border: 1px solid #faecd8;
  padding: 4px 12px;
  border-radius: 12px;
}

.tab-bar {
  display: flex;
  gap: 4px;
  padding: 12px 28px 0;
  border-bottom: 1px solid #ebeef5;
}

.tab-btn {
  padding: 10px 18px;
  border: none;
  background: transparent;
  font-size: 14px;
  color: #606266;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
}

.tab-btn:hover {
  color: #2e8565;
}

.tab-btn.active {
  color: #2e8565;
  font-weight: 600;
  border-bottom-color: #42b983;
}

.tab-body {
  padding: 20px 28px 28px;
}

/* 实时统计 */
.rt-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
  margin-bottom: 20px;
}

.rt-card {
  background: #fafbfc;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 22px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.rt-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}

.rt-value {
  font-size: 34px;
  font-weight: 700;
  color: #2c3e50;
  line-height: 1;
}

.rt-label {
  font-size: 13px;
  color: #909399;
}

/* 覆盖率 */
.cov-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
  margin-bottom: 24px;
}

.cov-card {
  background: #fafbfc;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 24px;
  text-align: center;
}

.cov-num {
  font-size: 38px;
  font-weight: 700;
  color: #2e8565;
  line-height: 1;
}

.cov-total {
  font-size: 16px;
  color: #a8abb2;
  font-weight: 400;
}

.cov-name {
  margin-top: 8px;
  font-size: 14px;
  color: #606266;
}

.cov-bar {
  height: 10px;
  background: #ebeef5;
  border-radius: 5px;
  margin: 14px 10px 8px;
  overflow: hidden;
}

.cov-inner {
  height: 100%;
  border-radius: 5px;
  background: linear-gradient(90deg, #42b983, #2e8565);
  transition: width 0.6s ease;
}

.cov-pct {
  font-size: 12px;
  color: #909399;
}

.cov-list-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.cov-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.cov-item {
  font-size: 13px;
  color: #2e8565;
  background: #f0f9f4;
  border: 1px solid #d9f0e4;
  padding: 6px 14px;
  border-radius: 16px;
}

@media (max-width: 900px) {
  .rt-grid { grid-template-columns: 1fr; }
  .cov-grid { grid-template-columns: 1fr; }
}
</style>
