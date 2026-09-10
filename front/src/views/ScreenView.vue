<template>
  <div class="screen">
    <header class="screen-header">
      <div class="header-side left">
        <span class="live-dot"></span>
        <span class="live-text">数据实时更新</span>
      </div>
      <h1 class="screen-title">东软环保公众监督系统 · 数据可视化大屏</h1>
      <div class="header-side right">
        <span class="clock">{{ clock }}</span>
        <button class="exit-btn" @click="$router.push('/')">退出大屏</button>
      </div>
    </header>

    <main class="screen-body">
      <!-- 左列 -->
      <section class="col">
        <div class="panel">
          <h3 class="panel-title"><i class="fa-solid fa-flag"></i>省分组超标统计</h3>
          <VChart :option="provinceOption" height="100%" class="chart" />
        </div>
        <div class="panel">
          <h3 class="panel-title"><i class="fa-solid fa-chart-pie"></i>AQI 指数分布统计</h3>
          <VChart :option="distributionOption" height="100%" class="chart" />
        </div>
      </section>

      <!-- 中列 -->
      <section class="col middle">
        <div class="kpi-row">
          <div class="kpi">
            <div class="kpi-num">{{ realtime.total }}</div>
            <div class="kpi-label">AQI检测累计数量</div>
          </div>
          <div class="kpi good">
            <div class="kpi-num">{{ realtime.good }}</div>
            <div class="kpi-label">检测结果良好</div>
          </div>
          <div class="kpi bad">
            <div class="kpi-num">{{ realtime.exceed }}</div>
            <div class="kpi-label">检测结果超标</div>
          </div>
          <div class="kpi">
            <div class="kpi-num">{{ provincePercent }}<small>%</small></div>
            <div class="kpi-label">省份网格覆盖率</div>
          </div>
          <div class="kpi">
            <div class="kpi-num">{{ cityPercent }}<small>%</small></div>
            <div class="kpi-label">大城市覆盖率</div>
          </div>
        </div>
        <div class="panel grow">
          <h3 class="panel-title"><i class="fa-solid fa-chart-line"></i>近12个月 AQI 超标趋势</h3>
          <VChart :option="trendOption" height="100%" class="chart" />
        </div>
        <div class="panel">
          <h3 class="panel-title"><i class="fa-solid fa-map"></i>全国网格覆盖情况</h3>
          <div class="cov-row">
            <div class="cov-block">
              <span class="cov-big">{{ coverage.provinceCovered }}<small>/{{ coverage.provinceTotal }} 省</small></span>
              <div class="cov-bar"><div class="cov-inner" :style="{ width: provincePercent + '%' }"></div></div>
            </div>
            <div class="cov-block">
              <span class="cov-big">{{ coverage.cityCovered }}<small>/{{ coverage.cityTotal }} 个大城市</small></span>
              <div class="cov-bar"><div class="cov-inner city" :style="{ width: cityPercent + '%' }"></div></div>
            </div>
          </div>
        </div>
      </section>

      <!-- 右列 -->
      <section class="col">
        <div class="panel">
          <h3 class="panel-title"><i class="fa-solid fa-gauge-high"></i>空气质量检测实时统计</h3>
          <VChart :option="realtimePieOption" height="100%" class="chart" />
        </div>
        <div class="panel">
          <h3 class="panel-title"><i class="fa-solid fa-city"></i>已覆盖网格区域</h3>
          <div class="region-list">
            <span v-for="(c, i) in coverage.coveredList" :key="i" class="region-item">
              {{ c.province }} · {{ c.city }}
            </span>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script>
import VChart from '../components/VChart.vue'
import {
  getProvinceStats, getDistributionStats, getTrendStats,
  getRealtimeStats, getCoverageStats
} from '../api/stats'

const DARK_TEXT = 'rgba(255,255,255,0.75)'

export default {
  name: 'ScreenView',
  components: { VChart },
  data() {
    return {
      clock: '',
      clockTimer: null,
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
        backgroundColor: 'transparent',
        color: ['#ff6e76', '#ffd68f', '#69d2ff', '#4be3a5'],
        textStyle: { color: DARK_TEXT },
        tooltip: { trigger: 'axis', backgroundColor: 'rgba(20,40,60,0.9)', borderColor: 'rgba(255,255,255,0.2)', textStyle: { color: '#fff' } },
        legend: { data: ['SO₂', 'CO', 'PM2.5', 'AQI等级'], top: 0, textStyle: { color: DARK_TEXT } },
        grid: { left: 40, right: 12, top: 44, bottom: 46 },
        xAxis: { type: 'category', data: this.provinceRows.map(r => r.province), axisLabel: { color: DARK_TEXT, fontSize: 10 }, axisLine: { lineStyle: { color: 'rgba(255,255,255,0.2)' } } },
        yAxis: { type: 'value', axisLabel: { color: DARK_TEXT }, splitLine: { lineStyle: { color: 'rgba(255,255,255,0.08)' } } },
        series: [
          { name: 'SO₂', type: 'bar', stack: 'x', data: this.provinceRows.map(r => r.so2), barMaxWidth: 20 },
          { name: 'CO', type: 'bar', stack: 'x', data: this.provinceRows.map(r => r.co), barMaxWidth: 20 },
          { name: 'PM2.5', type: 'bar', stack: 'x', data: this.provinceRows.map(r => r.pm25), barMaxWidth: 20 },
          { name: 'AQI等级', type: 'bar', stack: 'x', data: this.provinceRows.map(r => r.aqi), barMaxWidth: 20 }
        ]
      }
    },
    distributionOption() {
      return {
        backgroundColor: 'transparent',
        color: ['#4be3a5', '#69d2ff', '#ffd68f', '#ff9f7f', '#ff6e76', '#8d98b3'],
        tooltip: { trigger: 'item', backgroundColor: 'rgba(20,40,60,0.9)', borderColor: 'rgba(255,255,255,0.2)', textStyle: { color: '#fff' } },
        legend: { orient: 'vertical', right: 6, top: 'middle', textStyle: { color: DARK_TEXT, fontSize: 11 } },
        series: [
          {
            name: 'AQI分布',
            type: 'pie',
            radius: ['45%', '70%'],
            center: ['40%', '52%'],
            label: { show: false },
            data: this.distribution
          }
        ]
      }
    },
    trendOption() {
      return {
        backgroundColor: 'transparent',
        color: ['#ffd68f'],
        textStyle: { color: DARK_TEXT },
        tooltip: { trigger: 'axis', backgroundColor: 'rgba(20,40,60,0.9)', borderColor: 'rgba(255,255,255,0.2)', textStyle: { color: '#fff' } },
        grid: { left: 50, right: 24, top: 30, bottom: 34 },
        xAxis: { type: 'category', data: this.trend.map(t => t.month), boundaryGap: false, axisLabel: { color: DARK_TEXT }, axisLine: { lineStyle: { color: 'rgba(255,255,255,0.2)' } } },
        yAxis: { type: 'value', axisLabel: { color: DARK_TEXT }, splitLine: { lineStyle: { color: 'rgba(255,255,255,0.08)' } } },
        series: [
          {
            name: 'AQI超标累计',
            type: 'line',
            data: this.trend.map(t => t.exceed),
            smooth: true,
            areaStyle: {
              color: {
                type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
                colorStops: [
                  { offset: 0, color: 'rgba(255,214,143,0.4)' },
                  { offset: 1, color: 'rgba(255,214,143,0)' }
                ]
              }
            },
            lineStyle: { width: 3 },
            symbolSize: 7
          }
        ]
      }
    },
    realtimePieOption() {
      return {
        backgroundColor: 'transparent',
        color: ['#4be3a5', '#ff6e76'],
        tooltip: { trigger: 'item', backgroundColor: 'rgba(20,40,60,0.9)', borderColor: 'rgba(255,255,255,0.2)', textStyle: { color: '#fff' } },
        legend: { bottom: 0, textStyle: { color: DARK_TEXT } },
        series: [
          {
            name: '检测结果',
            type: 'pie',
            radius: ['45%', '68%'],
            center: ['50%', '46%'],
            label: { show: false },
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
    this.clockTimer = setInterval(() => {
      this.clock = new Date().toLocaleString('zh-CN', { hour12: false })
    }, 1000)
    const [p, d, t, r, c] = await Promise.allSettled([
      getProvinceStats(), getDistributionStats(), getTrendStats(),
      getRealtimeStats(), getCoverageStats()
    ])
    if (p.status === 'fulfilled') this.provinceRows = p.value.data
    if (d.status === 'fulfilled') this.distribution = d.value.data
    if (t.status === 'fulfilled') this.trend = t.value.data
    if (r.status === 'fulfilled') this.realtime = r.value.data
    if (c.status === 'fulfilled') this.coverage = c.value.data
  },
  beforeUnmount() {
    if (this.clockTimer) clearInterval(this.clockTimer)
  }
}
</script>

<style scoped>
.screen {
  min-height: 100vh;
  background: radial-gradient(ellipse at top, #10314a 0%, #0a1b2e 55%, #06121f 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  padding: 16px 20px 20px;
}

.screen-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(75, 227, 165, 0.25);
  margin-bottom: 14px;
}

.screen-title {
  margin: 0;
  font-size: 24px;
  letter-spacing: 3px;
  background: linear-gradient(90deg, #4be3a5, #69d2ff);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.header-side {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 220px;
}

.header-side.right {
  justify-content: flex-end;
}

.live-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #4be3a5;
  box-shadow: 0 0 10px #4be3a5;
  animation: pulse 1.6s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.live-text {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.55);
}

.clock {
  font-family: monospace;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.75);
}

.exit-btn {
  padding: 6px 14px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  background: transparent;
  color: rgba(255, 255, 255, 0.75);
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.exit-btn:hover {
  color: #fff;
  border-color: #4be3a5;
}

.screen-body {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 1.35fr 1fr;
  gap: 14px;
  min-height: 0;
}

.col {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 0;
}

.panel {
  flex: 1;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(75, 227, 165, 0.18);
  border-radius: 10px;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.panel.grow {
  flex: 1.4;
}

.panel-title {
  margin: 0 0 6px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.85);
  font-weight: 600;
}

.chart {
  flex: 1;
  min-height: 0;
}

/* KPI 行 */
.kpi-row {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}

.kpi {
  background: rgba(75, 227, 165, 0.08);
  border: 1px solid rgba(75, 227, 165, 0.25);
  border-radius: 10px;
  padding: 14px 8px;
  text-align: center;
}

.kpi-num {
  font-size: 30px;
  font-weight: 700;
  color: #4be3a5;
  line-height: 1.1;
}

.kpi-num small {
  font-size: 14px;
  font-weight: 400;
}

.kpi.good .kpi-num { color: #69d2ff; }
.kpi.bad .kpi-num { color: #ff6e76; }

.kpi-label {
  margin-top: 6px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.55);
}

/* 覆盖率 */
.cov-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  padding: 10px 6px;
}

.cov-big {
  font-size: 24px;
  font-weight: 700;
  color: #4be3a5;
}

.cov-big small {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.55);
  font-weight: 400;
  margin-left: 6px;
}

.cov-bar {
  height: 8px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  margin-top: 10px;
  overflow: hidden;
}

.cov-inner {
  height: 100%;
  border-radius: 4px;
  background: linear-gradient(90deg, #4be3a5, #69d2ff);
  transition: width 0.6s;
}

.cov-inner.city {
  background: linear-gradient(90deg, #ffd68f, #ff9f7f);
}

/* 区域列表 */
.region-list {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-content: flex-start;
  overflow-y: auto;
  padding: 4px 2px;
}

.region-item {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
  background: rgba(105, 210, 255, 0.1);
  border: 1px solid rgba(105, 210, 255, 0.3);
  padding: 5px 12px;
  border-radius: 14px;
}

@media (max-width: 1000px) {
  .screen-body { grid-template-columns: 1fr; }
}
.panel-title i {
  color: #4be3a5;
  margin-right: 6px;
  font-size: 12px;
}
</style>
