<template>
  <div class="nep-page">
    <div class="nep-card">
      <div class="nep-card-header">
        <div>
          <h2 class="nep-card-title">
            <span class="nep-title-icon"><i class="fa-solid fa-chart-column"></i></span>
            统计数据管理
          </h2>
          <p class="nep-card-sub">
            五项统计：省分组超标 / AQI指数分布 / 12个月趋势 / 检测数量实时 / 全国网格覆盖率
            <el-tag v-if="mock" type="warning" size="small" effect="plain" style="margin-left:8px">演示数据</el-tag>
          </p>
        </div>
        <el-button :icon="Refresh" circle @click="loadAll" />
      </div>

      <div class="nep-card-body">
        <el-tabs v-model="activeTab" class="stats-tabs">
          <!-- 1. 省分组超标统计 -->
          <el-tab-pane label="省分组超标统计" name="province">
            <div class="chart-box"><VChart :option="provinceOption" /></div>
            <el-table :data="provinceRows" size="small" :header-cell-style="{ background: '#f8faf9' }"
              class="mini-table">
              <el-table-column prop="province" label="省份" min-width="120" />
              <el-table-column prop="so2" label="SO₂超标" width="100" align="center" />
              <el-table-column prop="co" label="CO超标" width="100" align="center" />
              <el-table-column prop="pm25" label="PM2.5超标" width="110" align="center" />
              <el-table-column prop="aqi" label="AQI等级超标" width="120" align="center" />
              <el-table-column prop="total" label="累计检测" width="100" align="center" />
            </el-table>
          </el-tab-pane>

          <!-- 2. AQI指数分布 -->
          <el-tab-pane label="AQI指数分布" name="distribution">
            <div class="split">
              <div class="chart-box tall"><VChart :option="distributionOption" /></div>
              <div class="dist-list">
                <div v-for="(item, i) in distribution" :key="i" class="dist-item">
                  <span class="dist-dot" :style="{ background: distColors[i] }"></span>
                  <span class="dist-name">{{ item.name }}</span>
                  <span class="dist-value">{{ item.value }} 条</span>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <!-- 3. AQI指数趋势 -->
          <el-tab-pane label="AQI指数趋势" name="trend">
            <p class="pane-tip">当前 12 个月内，每个月的全国 AQI 超标累计数量</p>
            <div class="chart-box tall"><VChart :option="trendOption" /></div>
          </el-tab-pane>

          <!-- 4. 实时统计 -->
          <el-tab-pane label="实时统计" name="realtime">
            <div class="rt-grid">
              <StatCard icon="fa-solid fa-flask-vial" label="AQI 检测累计数量" :value="realtime.total" color="#0ea5e9" />
              <StatCard icon="fa-solid fa-face-smile" label="检测结果良好累计" :value="realtime.good" color="#10b981" />
              <StatCard icon="fa-solid fa-face-dizzy" label="检测结果超标累计" :value="realtime.exceed" color="#ef4444" />
            </div>
            <div class="chart-box tall"><VChart :option="realtimePieOption" /></div>
          </el-tab-pane>

          <!-- 5. 网格覆盖率 -->
          <el-tab-pane label="网格覆盖率" name="coverage">
            <p class="pane-tip">当前使用本系统的网格区域在全国所有省 / 所有大城市中的覆盖率</p>
            <div class="cov-cards">
              <div class="cov-card">
                <div class="cov-head">
                  <i class="fa-solid fa-map"></i> 省份覆盖率
                </div>
                <div class="cov-num">{{ coverage.provinceCovered }}<small>/{{ coverage.provinceTotal }} 省</small></div>
                <el-progress :percentage="provincePercent" :stroke-width="12" />
              </div>
              <div class="cov-card">
                <div class="cov-head">
                  <i class="fa-solid fa-city"></i> 大城市覆盖率
                </div>
                <div class="cov-num">{{ coverage.cityCovered }}<small>/{{ coverage.cityTotal }} 个大城市</small></div>
                <el-progress :percentage="cityPercent" :stroke-width="12" color="#0ea5e9" />
              </div>
            </div>
            <div class="covered-chips">
              <el-tag v-for="(c, i) in coverage.coveredList" :key="i" effect="plain" class="cov-chip">
                <i class="fa-solid fa-location-dot" style="margin-right:4px"></i>{{ c.province }} · {{ c.city }}
              </el-tag>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<script>
import { Refresh } from '@element-plus/icons-vue'
import VChart from '../../components/VChart.vue'
import StatCard from '../../components/StatCard.vue'
import {
  getProvinceStats, getDistributionStats, getTrendStats,
  getRealtimeStats, getCoverageStats
} from '../../api/stats'

export default {
  name: 'StatsView',
  components: { VChart, StatCard },
  data() {
    return {
      Refresh,
      activeTab: 'province',
      mock: false,
      provinceRows: [],
      distribution: [],
      trend: [],
      realtime: { total: 0, good: 0, exceed: 0 },
      coverage: { provinceCovered: 0, provinceTotal: 34, cityCovered: 0, cityTotal: 106, coveredList: [] },
      distColors: ['#10b981', '#84cc16', '#f59e0b', '#f97316', '#ef4444', '#7f1d1d']
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
        color: ['#ef4444', '#f97316', '#f59e0b', '#10b981'],
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
        color: this.distColors,
        tooltip: { trigger: 'item', formatter: '{b}：{c} 条（{d}%）' },
        legend: { show: false },
        series: [
          {
            name: 'AQI等级分布',
            type: 'pie',
            radius: ['42%', '68%'],
            center: ['50%', '52%'],
            label: { formatter: '{d}%' },
            data: this.distribution
          }
        ]
      }
    },
    trendOption() {
      return {
        color: ['#f59e0b'],
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
        color: ['#10b981', '#ef4444'],
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
  created() {
    this.loadAll()
  },
  methods: {
    async loadAll() {
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
}
</script>

<style scoped>
.stats-tabs :deep(.el-tabs__item.is-active) {
  color: #047857;
}

.stats-tabs :deep(.el-tabs__active-bar) {
  background-color: #10b981;
}

.chart-box {
  height: 380px;
}

.chart-box.tall {
  height: 420px;
}

.pane-tip {
  margin: 2px 0 12px;
  font-size: 13px;
  color: #94a3b8;
}

.mini-table {
  margin-top: 8px;
}

.split {
  display: grid;
  grid-template-columns: 3fr 2fr;
  gap: 20px;
  align-items: center;
}

.dist-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.dist-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13.5px;
}

.dist-dot {
  width: 12px;
  height: 12px;
  border-radius: 4px;
}

.dist-name {
  flex: 1;
  color: #475569;
}

.dist-value {
  font-weight: 700;
  color: #0f172a;
  font-variant-numeric: tabular-nums;
}

.rt-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 12px;
}

/* 覆盖率 */
.cov-cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 20px;
}

.cov-card {
  border: 1px solid var(--nep-border);
  border-radius: 12px;
  padding: 18px 20px;
}

.cov-head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13.5px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 10px;
}

.cov-head i {
  color: #10b981;
}

.cov-num {
  font-size: 30px;
  font-weight: 800;
  color: #0f172a;
  margin-bottom: 10px;
  font-variant-numeric: tabular-nums;
}

.cov-num small {
  font-size: 13px;
  color: #94a3b8;
  font-weight: 500;
  margin-left: 4px;
}

.covered-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.cov-chip {
  border-radius: 8px;
}

@media (max-width: 1000px) {
  .split,
  .rt-grid,
  .cov-cards {
    grid-template-columns: 1fr;
  }
}
</style>
