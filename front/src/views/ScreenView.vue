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
        <button class="exit-btn" @click="exitScreen">退出大屏</button>
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

    <!-- 决策者决策依据（后端 /stats/workforce 与 /stats/feedbackCoverage） -->
    <section class="decision-row">
      <!-- 板块一：网格员人力与增员需求 -->
      <div class="panel decision-panel">
        <div class="decision-head">
          <h3 class="panel-title"><i class="fa-solid fa-users"></i>网格员人力与增员需求</h3>
          <div class="head-right">
            <el-tag v-if="workforce.mock" type="warning" size="small" effect="plain">演示数据</el-tag>
            <router-link v-if="isAdmin" class="panel-link" :to="{ name: 'hrGrid' }">人员详情 ›</router-link>
          </div>
        </div>

        <div class="decision-body">
          <!-- 人力总量指标 -->
          <div class="kpi-grid">
            <div v-for="m in workforceMetrics" :key="m.cls" class="mini" :class="m.cls">
              <div class="mini-num">{{ m.value }}</div>
              <div class="mini-label">{{ m.label }}</div>
            </div>
          </div>

          <!-- 是否增员：醒目提示 -->
          <div class="need-banner" :class="workforce.needMore ? 'warn' : 'ok'">
            <div class="need-main">
              <i :class="workforce.needMore ? 'fa-solid fa-triangle-exclamation' : 'fa-solid fa-circle-check'"></i>
              <span class="need-text">{{ workforce.needMore ? '需要增员' : '人力充足' }}</span>
              <span v-if="workforce.needMore" class="need-strong">建议增员 {{ workforce.suggestAdd }} 人</span>
            </div>
            <div class="need-sub">
              待指派任务 {{ workforce.pendingTasks }} 条 · 未处理增员请求 {{ workforce.pendingDemands }} 条 · 人均在办上限 {{ workforce.capacity }} 条
            </div>
          </div>

          <!-- 增员原因（为空则不显示） -->
          <div v-if="workforce.needReasons.length" class="sub-block">
            <div class="sub-label">增员依据</div>
            <ul class="reason-list">
              <li v-for="(r, i) in workforce.needReasons" :key="i">{{ r }}</li>
            </ul>
          </div>

          <!-- 缺员区域（未处理的增员请求） -->
          <div v-if="workforce.lackRegions.length" class="sub-block">
            <div class="sub-label">缺员区域（{{ workforce.lackRegions.length }}）</div>
            <div class="lack-list">
              <el-tooltip
                v-for="(r, i) in workforce.lackRegions"
                :key="i"
                effect="dark"
                placement="top"
                :content="r.reason || '未填写缺员原因'"
              >
                <span class="lack-item">
                  <i class="fa-solid fa-location-dot"></i>{{ regionName(r) }}
                </span>
              </el-tooltip>
            </div>
          </div>

          <!-- 待增援区域：有任务却无在岗网格员，决策者可直接提交增援申请 -->
          <div v-if="workforce.needWorkerRegions.length" class="sub-block">
            <div class="sub-label">待增援区域（{{ workforce.needWorkerRegions.length }}）</div>
            <div class="demand-list">
              <div
                v-for="r in workforce.needWorkerRegions"
                :key="r.provinceId + '-' + r.cityId"
                class="demand-item"
              >
                <span class="demand-region">
                  <i class="fa-solid fa-location-dot"></i>{{ regionName(r) }}
                  <em>待指派 {{ r.pendingTasks }} 条</em>
                </span>
                <el-tag v-if="r.hasDemand" type="warning" size="small" effect="plain">
                  已提交待处理 #{{ r.demandId }}
                </el-tag>
                <el-button
                  v-else
                  type="primary"
                  size="small"
                  :loading="applyingRegion === r.cityId"
                  @click="submitDemand(r)"
                >提交增援申请</el-button>
              </div>
            </div>
          </div>

          <!-- 各区域人力分布（前 8 个区域） -->
          <div v-if="workforce.regions.length" class="sub-block chart-block">
            <div class="sub-label">各区域在岗 / 忙碌人数（前 {{ workforceRegions.length }} 个区域）</div>
            <VChart :option="workforceOption" height="100%" class="chart" />
          </div>
        </div>
      </div>

      <!-- 板块二：反馈覆盖度环比 -->
      <div class="panel decision-panel">
        <div class="decision-head">
          <h3 class="panel-title"><i class="fa-solid fa-chart-column"></i>反馈覆盖度环比 · 哪里反馈多、哪里少</h3>
          <div class="head-right">
            <el-tag v-if="feedbackCoverage.mock" type="warning" size="small" effect="plain">演示数据</el-tag>
            <router-link v-if="isAdmin" class="panel-link" :to="{ name: 'adminFeedback' }">反馈管理 ›</router-link>
          </div>
        </div>

        <div class="decision-body">
          <!-- 总量环比 -->
          <div class="sum-row">
            <div class="sum-item">
              <div class="mini-label">{{ feedbackCoverage.currentMonth || '本月' }} 反馈总量</div>
              <div class="sum-num">{{ feedbackCoverage.currentTotal }}</div>
            </div>
            <div class="sum-vs">VS</div>
            <div class="sum-item">
              <div class="mini-label">{{ feedbackCoverage.previousMonth || '上月' }} 反馈总量</div>
              <div class="sum-num prev">{{ feedbackCoverage.previousTotal }}</div>
            </div>
            <div class="sum-item">
              <div class="mini-label">环比变化</div>
              <div class="sum-num" :class="deltaClass(feedbackCoverage.deltaPercent)">
                {{ percentText(feedbackCoverage.deltaPercent) }}
              </div>
            </div>
          </div>

          <!-- 反馈较多 / 较少 榜单 -->
          <div class="rank-row">
            <div class="rank-col">
              <div class="rank-head">
                <i class="fa-solid fa-arrow-up-long hot"></i>反馈较多（本月）
              </div>
              <div v-for="(c, i) in feedbackCoverage.more" :key="i" class="rank-item">
                <div class="rank-top">
                  <span class="rank-name">{{ regionName(c) }}</span>
                  <span class="rank-num">
                    本月 {{ c.current }}<i class="sep">/</i>上月 {{ c.previous }}
                    <b :class="deltaClass(c.delta)">{{ percentText(c.deltaPercent) }}</b>
                  </span>
                </div>
                <el-tooltip effect="dark" placement="top" :content="c.reason || '暂无结论'">
                  <div class="rank-reason">
                    <el-tag size="small" effect="dark" :type="reasonType(c.reason)">{{ reasonKind(c.reason) }}</el-tag>
                    <span class="rank-reason-text">{{ c.reason || '暂无结论' }}</span>
                  </div>
                </el-tooltip>
              </div>
              <div v-if="!feedbackCoverage.more.length" class="rank-empty">暂无数据</div>
            </div>

            <div class="rank-col">
              <div class="rank-head">
                <i class="fa-solid fa-arrow-down-long cold"></i>反馈较少（本月）
              </div>
              <div v-for="(c, i) in feedbackCoverage.less" :key="i" class="rank-item">
                <div class="rank-top">
                  <span class="rank-name">{{ regionName(c) }}</span>
                  <span class="rank-num">
                    本月 {{ c.current }}<i class="sep">/</i>上月 {{ c.previous }}
                    <b :class="deltaClass(c.delta)">{{ percentText(c.deltaPercent) }}</b>
                  </span>
                </div>
                <el-tooltip effect="dark" placement="top" :content="c.reason || '暂无结论'">
                  <div class="rank-reason">
                    <el-tag size="small" effect="dark" :type="reasonType(c.reason)">{{ reasonKind(c.reason) }}</el-tag>
                    <span class="rank-reason-text">{{ c.reason || '暂无结论' }}</span>
                  </div>
                </el-tooltip>
              </div>
              <div v-if="!feedbackCoverage.less.length" class="rank-empty">暂无数据</div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script>
import VChart from '../components/VChart.vue'
import { ElMessage } from 'element-plus'
import { roleHome } from '../constants/aqi'
import {
  getProvinceStats, getDistributionStats, getTrendStats,
  getRealtimeStats, getCoverageStats,
  getWorkforce, getFeedbackCoverage
} from '../api/stats'
import { applyGridDemand } from '../api/gridDemand'

const DARK_TEXT = 'rgba(255,255,255,0.75)'

// 反馈覆盖度结论标签的配色映射（key 为 reason 的结论前缀）
const KIND_TAG_TYPES = {
  '环境良好': 'success',
  '覆盖不足': 'danger',
  '反馈减少': 'warning',
  '反馈增加': 'primary',
  '基本持平': 'info'
}
// 网格员人力指标与结论标签的 el-tag 类型映射
const METRIC_TAG_TYPES = {
  workplace: 'success',
  busy: 'warning',
  idle: 'primary',
  leave: 'info',
  region: 'danger'
}

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
      coverage: { provinceCovered: 0, provinceTotal: 34, cityCovered: 0, cityTotal: 106, coveredList: [] },
      // 板块一：网格员人力与增员需求（后端 /stats/workforce）
      workforce: {
        mock: false,
        total: 0, working: 0, onLeave: 0, busy: 0, idle: 0, capacity: 0,
        pendingTasks: 0, pendingDemands: 0, suggestAdd: 0, needMore: false,
        needReasons: [], regions: [], lackRegions: [], needWorkerRegions: []
      },
      // 正在提交增援申请的区域 cityId（用于按钮 loading）
      applyingRegion: null,
      // 板块二：反馈覆盖度环比（后端 /stats/feedbackCoverage）
      feedbackCoverage: {
        mock: false,
        currentMonth: '', previousMonth: '',
        currentTotal: 0, previousTotal: 0, delta: 0, deltaPercent: null,
        more: [], less: []
      }
    }
  },
  methods: {
    /**
     * 刷新决策依据数据（网格员人力 + 反馈覆盖度环比）。
     * 实际取数逻辑在 options 层的 loadDecisionData（created 与本方法共用，避免复制两份）。
     */
    async refreshDecisionData() {
      return this.$options.loadDecisionData.call(this)
    },
    exitScreen() {
      const role = this.$store.getters.role
      if (role === 'viewer') {
        // 决策者的角色首页就是大屏本身，退出大屏即退出登录回登录页
        this.$store.dispatch('logout')
        this.$router.push({ name: 'login' })
      } else {
        this.$router.push(roleHome(role))
      }
    },
    // 某省某市的可读名称（兼容 provinceName/cityName 与 province/city 两种字段名）
    regionName(r) {
      if (!r) return '未知区域'
      const province = r.provinceName || r.province || ''
      const city = r.cityName || r.city || ''
      const name = `${province} ${city}`.trim()
      return name || '未知区域'
    },
    /**
     * 决策者直接提交增援申请。
     * 大屏上看不到具体反馈，因此按“区域”提交，后端会自动关联该区域最早的待指派反馈；
     * 同一区域已有待处理请求时后端会复用并返回原请求编号。
     * 注意：后端业务错误是 HTTP 200 + body.code != 200，必须显式判断 code。
     */
    async submitDemand(region) {
      if (!region || this.applyingRegion) return
      this.applyingRegion = region.cityId
      try {
        const res = await applyGridDemand({
          provinceId: region.provinceId,
          cityId: region.cityId,
          source: 'viewer'
        })
        const body = res && res.data
        if (body && body.code === 200) {
          ElMessage.success(body.message || '增援申请已提交')
          await this.refreshDecisionData()
        } else {
          ElMessage.error((body && body.message) || '提交增援申请失败')
        }
      } catch (err) {
        ElMessage.error((err && err.message) || '提交增援申请失败，请确认后端服务已启动')
      } finally {
        this.applyingRegion = null
      }
    },
    // 环比百分比展示：deltaPercent 为 null（上月无数据）时显示 —
    percentText(v) {
      if (v === null || v === undefined || v === '' || isNaN(v)) return '—'
      const n = Number(v)
      return (n > 0 ? '+' : '') + n.toFixed(1) + '%'
    },
    // 环比方向配色：上升红（提示关注）、下降绿、持平灰
    deltaClass(v) {
      if (v === null || v === undefined || v === '' || isNaN(v) || Number(v) === 0) return 'flat'
      return Number(v) > 0 ? 'up' : 'down'
    },
    // 后端结论分类，用于标签上的短标签与配色
    reasonKind(reason) {
      const text = reason || ''
      const keys = ['环境良好', '覆盖不足', '反馈减少', '反馈增加', '基本持平']
      for (let i = 0; i < keys.length; i++) {
        if (text.indexOf(keys[i]) === 0) return keys[i]
      }
      return text ? '其他' : '暂无结论'
    },
    reasonType(reason) {
      return KIND_TAG_TYPES[this.reasonKind(reason)] || 'info'
    }
  },
  computed: {
    // 仅系统管理员可跳转到人员 / 反馈管理页
    isAdmin() {
      return this.$store.getters.role === 'admin'
    },
    // 网格员人力指标卡片
    workforceMetrics() {
      const w = this.workforce
      return [
        { label: '网格员总数', value: w.total, cls: '' },
        { label: '在岗', value: w.working, cls: 'good' },
        { label: '忙碌', value: w.busy, cls: 'warn' },
        { label: '空闲', value: w.idle, cls: 'info' },
        { label: '请假中（非在岗）', value: w.onLeave, cls: 'bad' }
      ]
    },
    // 人力分布图只取前 8 个区域（按在岗人数降序，其次按总数降序），避免横轴过密
    // 注意：后端对一个区域可能有 2 条记录（如各 1 人的网格），排序并不能合并它们
    workforceRegions() {
      const list = this.workforce.regions || []
      return list.slice()
        .sort((a, b) => ((Number(b.working) || 0) - (Number(a.working) || 0)) ||
          ((Number(b.total) || 0) - (Number(a.total) || 0)))
        .slice(0, 8)
    },
    // 各区域在岗 / 忙碌人数堆叠柱状图
    workforceOption() {
      const rows = this.workforceRegions
      // 图表内省市同名较多，横轴统一用“省·市”，tooltip 再给出在岗/空闲明细
      const names = rows.map(r => (r.provinceName && r.cityName) ? (r.provinceName + '·' + r.cityName) : (r.cityName || r.provinceName || '未知'))
      return {
        backgroundColor: 'transparent',
        color: ['#4be3a5', '#ffd68f'],
        textStyle: { color: DARK_TEXT },
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' },
          backgroundColor: 'rgba(20,40,60,0.9)',
          borderColor: 'rgba(255,255,255,0.2)',
          textStyle: { color: '#fff' },
          // 补充在岗/空闲明细，便于决策者判断忙碌程度
          formatter(params) {
            if (!params || !params.length) return ''
            const row = rows[params[0].dataIndex] || {}
            const lines = [params[0].axisValue]
            params.forEach(p => lines.push(p.marker + p.seriesName + '：' + p.value + ' 人'))
            lines.push('空闲：' + (row.idle || 0) + ' 人 / 总数：' + (row.total || 0) + ' 人')
            return lines.join('<br/>')
          }
        },
        legend: { data: ['在岗', '忙碌'], right: 0, top: 0, textStyle: { color: DARK_TEXT, fontSize: 11 } },
        grid: { left: 44, right: 10, top: 36, bottom: 42 },
        xAxis: {
          type: 'category',
          data: names,
          axisLabel: { color: DARK_TEXT, fontSize: 10, interval: 0, rotate: 30 },
          axisLine: { lineStyle: { color: 'rgba(255,255,255,0.2)' } }
        },
        yAxis: {
          type: 'value',
          name: '人',
          nameTextStyle: { color: DARK_TEXT, fontSize: 10 },
          axisLabel: { color: DARK_TEXT },
          splitLine: { lineStyle: { color: 'rgba(255,255,255,0.08)' } }
        },
        series: [
          { name: '在岗', type: 'bar', stack: 'people', barMaxWidth: 22, data: rows.map(r => r.working) },
          { name: '忙碌', type: 'bar', stack: 'people', barMaxWidth: 22, data: rows.map(r => r.busy) }
        ]
      }
    },
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

    // 决策依据：网格员人力与增员需求 + 反馈覆盖度环比（提交增援申请后需要重新拉取）
    await this.refreshDecisionData()
  },
  // 决策依据取数（网格员人力 + 反馈覆盖度环比）。
  // 说明：该函数位于组件 options 层，methods 内通过 $options 调用（见 methods.refreshDecisionData），
  // 这样 created() 与“提交增援申请”后的刷新共用同一份逻辑，不复制代码。
  async loadDecisionData() {
    // 后端业务错误是 HTTP 200 + body.code != 200，且后端未启动时可能直接抛错，
    // 因此统一用 allSettled 兜底，任何一侧失败都不影响整页渲染。
    const [w, f] = await Promise.allSettled([getWorkforce(), getFeedbackCoverage()])
    if (w.status === 'fulfilled' && w.value.data && w.value.data.code === 200) {
      const wf = w.value.data.data || {}
      this.workforce = {
        mock: !!w.value.data.mock,
        total: wf.total || 0,
        working: wf.working || 0,
        onLeave: wf.onLeave || 0,
        busy: wf.busy || 0,
        idle: wf.idle || 0,
        capacity: wf.capacity || 0,
        pendingTasks: wf.pendingTasks || 0,
        pendingDemands: wf.pendingDemands || 0,
        suggestAdd: wf.suggestAdd || 0,
        needMore: !!wf.needMore,
        needReasons: wf.needReasons || [],
        regions: wf.regions || [],
        lackRegions: wf.lackRegions || [],
        needWorkerRegions: wf.needWorkerRegions || []
      }
    }
    if (f.status === 'fulfilled' && f.value.data && f.value.data.code === 200) {
      const fc = f.value.data.data || {}
      const cur = fc.currentTotal || 0
      const prev = fc.previousTotal || 0
      const delta = fc.delta !== undefined && fc.delta !== null ? fc.delta : (cur - prev)
      this.feedbackCoverage = {
        mock: !!f.value.data.mock,
        currentMonth: fc.currentMonth || '',
        previousMonth: fc.previousMonth || '',
        currentTotal: cur,
        previousTotal: prev,
        delta: delta,
        // 后端未给 deltaPercent 时按总量自行计算，上月为 0 则保持 null 显示 —
        deltaPercent: fc.deltaPercent !== undefined && fc.deltaPercent !== null
          ? fc.deltaPercent
          : (prev ? Math.round(delta / prev * 1000) / 10 : null),
        more: (fc.more || []).slice(0, 5),
        less: (fc.less || []).slice(0, 5)
      }
    }
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

/* ===== 决策依据：两个新板块（网格员人力 / 反馈覆盖度环比） ===== */
.decision-row {
  display: grid;
  grid-template-columns: 1fr 1.35fr;
  gap: 14px;
  margin-top: 14px;
}

.decision-panel {
  height: 372px;
  padding-bottom: 10px;
}

.decision-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 6px;
}

.decision-head .panel-title {
  margin: 0;
}

.head-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.panel-link {
  font-size: 12px;
  color: #69d2ff;
  text-decoration: none;
}

.panel-link:hover {
  color: #4be3a5;
}

.decision-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-right: 2px;
}

/* 人力指标小卡片 */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
}

.mini {
  background: rgba(75, 227, 165, 0.08);
  border: 1px solid rgba(75, 227, 165, 0.25);
  border-radius: 8px;
  padding: 6px 4px;
  text-align: center;
  min-width: 0;
}

.mini-num {
  font-size: 20px;
  font-weight: 700;
  color: #4be3a5;
  line-height: 1.2;
}

.mini-label {
  margin-top: 2px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.55);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mini.good .mini-num { color: #69d2ff; }
.mini.warn .mini-num { color: #ffd68f; }
.mini.bad .mini-num { color: #ff6e76; }
.mini.info .mini-num { color: #8d98b3; }

/* 是否增员提示条 */
.need-banner {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 12px;
  border-radius: 8px;
  padding: 8px 12px;
}

.need-banner.warn {
  background: rgba(255, 110, 118, 0.14);
  border: 1px solid rgba(255, 110, 118, 0.45);
}

.need-banner.ok {
  background: rgba(75, 227, 165, 0.12);
  border: 1px solid rgba(75, 227, 165, 0.42);
}

.need-main {
  display: flex;
  align-items: center;
  gap: 8px;
}

.need-banner.warn .need-main { color: #ff9ca2; }
.need-banner.ok .need-main { color: #4be3a5; }

.need-text {
  font-size: 15px;
  font-weight: 700;
}

.need-strong {
  background: #ff6e76;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  border-radius: 10px;
  padding: 2px 10px;
}

.need-sub {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.62);
}

/* 子区块 */
.sub-block {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sub-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.sub-label::before {
  content: '';
  display: inline-block;
  width: 3px;
  height: 10px;
  background: #4be3a5;
  border-radius: 2px;
  margin-right: 6px;
  vertical-align: -1px;
}

.reason-list {
  margin: 0;
  padding-left: 18px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.7;
}

.lack-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.lack-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #ff9ca2;
  background: rgba(255, 110, 118, 0.12);
  border: 1px solid rgba(255, 110, 118, 0.4);
  border-radius: 12px;
  padding: 3px 10px;
  cursor: help;
}

.chart-block {
  flex: 1;
  min-height: 158px;
}

/* 反馈总量环比 */
.sum-row {
  display: grid;
  grid-template-columns: 1fr auto 1fr 1fr;
  align-items: center;
  gap: 10px;
  background: rgba(105, 210, 255, 0.08);
  border: 1px solid rgba(105, 210, 255, 0.22);
  border-radius: 8px;
  padding: 8px 12px;
  text-align: center;
}

.sum-num {
  font-size: 22px;
  font-weight: 700;
  color: #69d2ff;
  line-height: 1.2;
}

.sum-num.prev {
  color: rgba(255, 255, 255, 0.7);
}

.sum-num.up { color: #ff6e76; }
.sum-num.down { color: #4be3a5; }
.sum-num.flat { color: rgba(255, 255, 255, 0.7); }

.sum-vs {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
}

/* 反馈较多 / 较少 榜单 */
.rank-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.rank-col {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.rank-head {
  font-size: 12.5px;
  color: rgba(255, 255, 255, 0.82);
  padding-bottom: 4px;
  border-bottom: 1px dashed rgba(255, 255, 255, 0.14);
}

.rank-head i.hot { color: #ff6e76; margin-right: 5px; }
.rank-head i.cold { color: #69d2ff; margin-right: 5px; }

.rank-item {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  padding: 6px 8px;
}

.rank-top {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}

.rank-name {
  font-size: 12.5px;
  color: rgba(255, 255, 255, 0.88);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rank-num {
  flex-shrink: 0;
  font-size: 11.5px;
  color: rgba(255, 255, 255, 0.6);
}

.rank-num i.sep {
  font-style: normal;
  margin: 0 5px;
  color: rgba(255, 255, 255, 0.25);
}

.rank-num b {
  margin-left: 6px;
  font-weight: 700;
}

.rank-num b.up { color: #ff6e76; }
.rank-num b.down { color: #4be3a5; }
.rank-num b.flat { color: rgba(255, 255, 255, 0.6); }

/* reason 可能很长：标签短名 + 文本截断，完整内容由 el-tooltip 展示 */
.rank-reason {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  min-width: 0;
}

.rank-reason-text {
  flex: 1;
  min-width: 0;
  font-size: 11.5px;
  color: rgba(255, 255, 255, 0.62);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rank-empty {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
  padding: 8px 0;
}

/* 滚动条与标签微调 */
.decision-body::-webkit-scrollbar {
  width: 6px;
}

.decision-body::-webkit-scrollbar-thumb {
  background: rgba(75, 227, 165, 0.3);
  border-radius: 3px;
}

.decision-body :deep(.el-tag) {
  flex-shrink: 0;
}

@media (max-width: 1200px) {
  .decision-row { grid-template-columns: 1fr; }
  .decision-panel { height: auto; min-height: 300px; }
}
</style>
