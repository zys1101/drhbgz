<template>
  <div class="nep-page dashboard">
    <!-- 欢迎横幅 -->
    <section class="hero nep-card">
      <div class="hero-main">
        <h2>{{ greeting }}，{{ username }}</h2>
        <p>这里是系统运行总览看板，随时掌握公众反馈处理进展与全国 AQI 检测情况。</p>
        <div class="hero-actions">
          <el-button type="primary" class="nep-btn-gradient" @click="$router.push('/admin/feedback')">
            <i class="fa-solid fa-inbox" style="margin-right:6px"></i>反馈管理
          </el-button>
          <el-button @click="$router.push('/admin/stats')">
            <i class="fa-solid fa-chart-column" style="margin-right:6px"></i>统计报表
          </el-button>
        </div>
      </div>
      <div class="hero-art"><i class="fa-solid fa-earth-asia"></i></div>
    </section>

    <!-- KPI 指标 -->
    <section class="kpi-grid">
      <StatCard icon="fa-solid fa-inbox" label="反馈总数" :value="statText(feedbackTotal)"
        color="#10b981" deco="fa-solid fa-inbox" />
      <StatCard icon="fa-solid fa-hourglass-half" label="待处理反馈" :value="statText(pendingTotal)"
        color="#f59e0b" deco="fa-solid fa-hourglass-half" />
      <StatCard icon="fa-solid fa-circle-check" label="已确认完成" :value="statText(doneTotal)"
        color="#6366f1" deco="fa-solid fa-circle-check" />
      <StatCard icon="fa-solid fa-flask-vial" label="实测AQI数据" :value="statText(dataTotal)"
        color="#0ea5e9" deco="fa-solid fa-flask-vial" />
    </section>

    <!-- 双面板 -->
    <section class="panel-grid">
      <div class="nep-card">
        <div class="nep-card-header">
          <h3 class="nep-card-title"><span class="nep-title-icon"><i class="fa-solid fa-table-cells"></i></span>AQI 级别标准</h3>
          <el-link type="primary" :underline="false" @click="$router.push('/aqi')">管理 <i class="fa-solid fa-angle-right"></i></el-link>
        </div>
        <div class="nep-card-body legend-list">
          <div v-for="item in aqiLevels" :key="item.aqiId" class="legend-item">
            <span class="legend-color" :style="{ background: item.color }"></span>
            <div class="legend-meta">
              <span class="legend-name">{{ item.chineseExplain }} · {{ item.aqiExplain }}</span>
              <span class="legend-range">AQI {{ item.aqiRange }}</span>
            </div>
          </div>
          <div v-if="!aqiLevels.length" class="nep-empty-sm">{{ backendTip }}</div>
        </div>
      </div>

      <div class="nep-card">
        <div class="nep-card-header">
          <h3 class="nep-card-title"><span class="nep-title-icon"><i class="fa-solid fa-clock-rotate-left"></i></span>最新反馈</h3>
          <el-link type="primary" :underline="false" @click="$router.push('/admin/feedback')">更多 <i class="fa-solid fa-angle-right"></i></el-link>
        </div>
        <div class="nep-table-wrap">
          <el-table :data="recentFeedback" style="width: 100%" :header-cell-style="{ background: '#f8faf9' }">
            <el-table-column label="网格区域" min-width="150">
              <template #default="{ row }">
                <span class="region">{{ row.provinceName || row.provinceId }} · {{ row.cityName || row.cityId }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="afDate" label="反馈时间" width="100" />
            <el-table-column label="预估等级" width="120" align="center">
              <template #default="{ row }">
                <GradeTag :grade="row.estimatedGrade" />
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <StateTag :state="row.state" />
              </template>
            </el-table-column>
          </el-table>
          <div v-if="!recentFeedback.length" class="nep-empty-sm">{{ backendTip }}</div>
        </div>
      </div>
    </section>
  </div>
</template>

<script>
import StatCard from '../components/StatCard.vue'
import GradeTag from '../components/GradeTag.vue'
import StateTag from '../components/StateTag.vue'
import { getAqiFeedbackList } from '../api/aqiFeedback.js'
import { getAqiList } from '../api/aqi.js'
import { getAqiDataList } from '../api/task.js'

export default {
  name: 'HomeView',
  components: { StatCard, GradeTag, StateTag },
  data() {
    return {
      feedbackTotal: null,
      pendingTotal: null,
      doneTotal: null,
      dataTotal: null,
      aqiLevels: [],
      recentFeedback: []
    }
  },
  computed: {
    username() {
      return this.$store.state.user ? this.$store.state.user.username : ''
    },
    greeting() {
      const h = new Date().getHours()
      if (h < 6) return '夜深了'
      if (h < 9) return '早上好'
      if (h < 12) return '上午好'
      if (h < 14) return '中午好'
      if (h < 18) return '下午好'
      return '晚上好'
    },
    backendTip() {
      return this.feedbackTotal === null && this.aqiTotalNull()
        ? '暂无数据（后端服务未连接时会显示此提示）'
        : '暂无数据'
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    aqiTotalNull() {
      return !this.aqiLevels.length
    },
    statText(v) {
      return v === null ? '—' : v
    },
    async loadData() {
      try {
        const [fbRes, aqiRes, dataRes] = await Promise.allSettled([
          getAqiFeedbackList(), getAqiList(), getAqiDataList()
        ])
        if (fbRes.status === 'fulfilled' && fbRes.value.data.code === 200) {
          const list = fbRes.value.data.data || []
          this.feedbackTotal = list.length
          this.pendingTotal = list.filter(i => i.state !== 3).length
          this.doneTotal = list.filter(i => i.state === 3).length
          this.recentFeedback = list.slice(0, 6)
        }
        if (aqiRes.status === 'fulfilled' && aqiRes.value.data.code === 200) {
          this.aqiLevels = aqiRes.value.data.data || []
        }
        if (dataRes.status === 'fulfilled' && dataRes.value.list) {
          this.dataTotal = dataRes.value.list.length
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
/* ---------------- 欢迎横幅 ---------------- */
.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 26px 30px;
  margin-bottom: 18px;
  background:
    radial-gradient(420px 200px at 92% 0%, rgba(16, 185, 129, 0.12), transparent 60%),
    linear-gradient(120deg, #ffffff 55%, #ecfdf5 100%);
}

.hero-main h2 {
  margin: 0;
  font-size: 21px;
  color: #0f172a;
}

.hero-main p {
  margin: 8px 0 16px;
  color: #64748b;
  font-size: 13.5px;
}

.hero-art {
  font-size: 96px;
  color: #10b981;
  opacity: 0.16;
  padding-right: 20px;
}

/* ---------------- KPI ---------------- */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 18px;
}

/* ---------------- 双面板 ---------------- */
.panel-grid {
  display: grid;
  grid-template-columns: 5fr 7fr;
  gap: 16px;
  align-items: start;
}

.legend-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 10px;
  transition: background 0.15s;
}

.legend-item:hover {
  background: #f8faf9;
}

.legend-color {
  width: 14px;
  height: 14px;
  border-radius: 5px;
  flex-shrink: 0;
  box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.06);
}

.legend-meta {
  display: flex;
  flex-direction: column;
}

.legend-name {
  font-size: 13.5px;
  font-weight: 600;
  color: #334155;
}

.legend-range {
  font-size: 12px;
  color: #94a3b8;
}

.region {
  font-size: 13px;
  color: #334155;
}

.nep-empty-sm {
  padding: 34px 10px;
  text-align: center;
  color: #b6c2c7;
  font-size: 13px;
}

@media (max-width: 1100px) {
  .kpi-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .panel-grid {
    grid-template-columns: 1fr;
  }
}
</style>
