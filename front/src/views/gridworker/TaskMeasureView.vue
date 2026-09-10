<template>
  <div class="nep-page">
    <div v-if="task" class="nep-card form-card">
      <div class="nep-card-header">
        <div>
          <h2 class="nep-card-title">
            <span class="nep-title-icon"><i class="fa-solid fa-vials"></i></span>
            录入实测 AQI 数据
          </h2>
          <p class="nep-card-sub">任务编号 {{ task.taskId || task.afId }} · 到达网格区域实地检测后如实录入</p>
        </div>
        <el-button text @click="$router.push('/gw/tasks')">
          <i class="fa-solid fa-arrow-left"></i> 返回任务列表
        </el-button>
      </div>

      <div class="nep-card-body">
        <!-- 任务信息卡 -->
        <div class="task-brief">
          <div class="brief-region">
            <i class="fa-solid fa-location-dot"></i>
            <b>{{ task.provinceName }} · {{ task.cityName }}</b>
          </div>
          <span class="brief-addr">{{ task.address }}</span>
          <div class="brief-meta">
            <span>预估等级</span>
            <GradeTag :grade="task.estimatedGrade" />
            <span class="brief-dot">·</span>
            <span>反馈时间 {{ task.afDate }} {{ task.afTime }}</span>
          </div>
          <p class="brief-info">“{{ task.information }}”</p>
        </div>

        <!-- 检测流程 -->
        <el-steps :active="completedCount" align-center class="steps" finish-status="success">
          <el-step v-for="p in pollutants" :key="p.key" :title="p.short" description="浓度等级" />
        </el-steps>

        <div class="measure-grid">
          <div v-for="p in pollutants" :key="p.key" class="measure-item"
            :class="{ done: measure[p.key] > 0 }">
            <div class="measure-label">
              <i class="fa-solid fa-industry"></i>{{ p.label }}
            </div>
            <el-select v-model="measure[p.key]" placeholder="选择浓度等级" size="large" style="width: 100%">
              <el-option v-for="g in gradeOptions" :key="g.value" :value="g.value" :label="g.label" />
            </el-select>
          </div>
        </div>

        <!-- AQI 结果 -->
        <transition name="pop">
          <div v-if="aqiGrade > 0" class="aqi-result" :style="resultStyle">
            <div class="aqi-result-label">系统按 AQI = MAX（SO2AQI，COAQI，PM2.5AQI）自动计算</div>
            <div class="aqi-result-value">
              当前网格区域 AQI 等级：<b>{{ gradeText(aqiGrade) }}</b>
            </div>
          </div>
        </transition>

        <div class="form-actions">
          <el-button size="large" @click="resetMeasure">
            <i class="fa-solid fa-rotate-left" style="margin-right:6px"></i>重新录入
          </el-button>
          <el-button type="primary" size="large" class="nep-btn-gradient" :loading="submitting" @click="handleSubmit">
            <i class="fa-solid fa-paper-plane" style="margin-right:6px"></i>提交实测数据
          </el-button>
        </div>
      </div>
    </div>

    <div v-else class="nep-card">
      <div class="nep-empty">
        <span class="nep-empty-icon"><i class="fa-solid fa-triangle-exclamation"></i></span>
        <span>未找到任务数据，请从任务列表进入</span>
        <el-button type="primary" text @click="$router.push('/gw/tasks')">返回任务列表</el-button>
      </div>
    </div>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import GradeTag from '../../components/GradeTag.vue'
import { submitMeasure } from '../../api/task'
import { AQI_GRADES, gradeText, calcAqiGrade } from '../../constants/aqi'

const RESULT_COLORS = {
  1: { bg: '#f0fdf4', border: '#bbf7d0', color: '#16a34a' },
  2: { bg: '#f7fee7', border: '#d9f99d', color: '#65a30d' },
  3: { bg: '#fffbeb', border: '#fde68a', color: '#d97706' },
  4: { bg: '#fef2f2', border: '#fecaca', color: '#dc2626' },
  5: { bg: '#fff1f2', border: '#fda4af', color: '#be123c' },
  6: { bg: '#fde8e8', border: '#fca5a5', color: '#7f1d1d' }
}

export default {
  name: 'TaskMeasureView',
  components: { GradeTag },
  data() {
    return {
      task: null,
      pollutants: [
        { key: 'so2Grade', label: 'SO₂ 二氧化硫 AQI 浓度等级', short: 'SO₂' },
        { key: 'coGrade', label: 'CO 一氧化碳 AQI 浓度等级', short: 'CO' },
        { key: 'pm25Grade', label: 'PM2.5 悬浮颗粒物 AQI 浓度等级', short: 'PM2.5' }
      ],
      measure: {
        so2Grade: 0,
        coGrade: 0,
        pm25Grade: 0
      },
      gradeOptions: AQI_GRADES,
      submitting: false
    }
  },
  computed: {
    aqiGrade() {
      return calcAqiGrade(this.measure.so2Grade, this.measure.coGrade, this.measure.pm25Grade)
    },
    completedCount() {
      return ['so2Grade', 'coGrade', 'pm25Grade'].filter(k => this.measure[k] > 0).length
    },
    resultStyle() {
      const c = RESULT_COLORS[this.aqiGrade] || RESULT_COLORS[1]
      return { background: c.bg, borderColor: c.border, color: c.color }
    }
  },
  created() {
    const raw = this.$route.query.data
    if (raw) {
      try {
        this.task = JSON.parse(decodeURIComponent(raw))
      } catch (e) {
        this.task = null
      }
    }
  },
  methods: {
    gradeText,
    resetMeasure() {
      this.measure = { so2Grade: 0, coGrade: 0, pm25Grade: 0 }
    },
    async handleSubmit() {
      if (this.aqiGrade === 0) {
        ElMessage.warning('请完整录入三项检测数据')
        return
      }
      this.submitting = true
      try {
        await submitMeasure(this.task, { ...this.measure, aqiGrade: this.aqiGrade })
        ElMessage.success('提交成功，任务已完成，数据已进入确认AQI数据列表')
        setTimeout(() => this.$router.push('/gw/tasks'), 1000)
      } catch (err) {
        console.error(err)
        ElMessage.error((err && err.message) || '提交失败，请重试')
        this.submitting = false
      }
    }
  }
}
</script>

<style scoped>
.form-card {
  max-width: 860px;
  margin: 20px auto 0;
}

/* 任务信息卡 */
.task-brief {
  background: linear-gradient(120deg, #f8fafc, #f0fdf9);
  border: 1px solid var(--nep-border);
  border-radius: 12px;
  padding: 16px 20px;
  margin-bottom: 22px;
}

.brief-region {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #047857;
  font-size: 15px;
}

.brief-region i {
  font-size: 13px;
}

.brief-addr {
  display: block;
  margin: 5px 0 8px 21px;
  color: #475569;
  font-size: 13px;
}

.brief-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 21px;
  font-size: 12.5px;
  color: #94a3b8;
  flex-wrap: wrap;
}

.brief-info {
  margin: 8px 0 0 21px;
  color: #64748b;
  font-size: 13px;
  font-style: italic;
}

/* 步骤条 */
.steps {
  margin: 6px 0 24px;
}

/* 三项检测 */
.measure-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.measure-item {
  border: 1.5px solid var(--nep-border);
  border-radius: 12px;
  padding: 16px;
  transition: all 0.2s;
}

.measure-item.done {
  border-color: #6ee7b7;
  background: #f7fdfa;
}

.measure-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 12px;
}

.measure-label i {
  color: #10b981;
}

/* AQI 结果横幅 */
.aqi-result {
  margin-top: 20px;
  border: 1px solid;
  border-radius: 12px;
  padding: 14px 18px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.aqi-result-label {
  font-size: 12px;
  opacity: 0.75;
}

.aqi-result-value {
  font-size: 14.5px;
}

.aqi-result-value b {
  font-size: 17px;
}

.pop-enter-active {
  transition: all 0.25s ease;
}

.pop-enter-from {
  opacity: 0;
  transform: translateY(6px);
}

.form-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 22px;
}

@media (max-width: 760px) {
  .measure-grid {
    grid-template-columns: 1fr;
  }
}
</style>
