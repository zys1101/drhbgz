<template>
  <div class="page">
    <transition name="toast">
      <div v-if="toast.show" class="toast" :class="toast.type">
        <span class="toast-msg">{{ toast.message }}</span>
      </div>
    </transition>

    <div class="card">
      <div class="card-header">
        <button class="back-btn" @click="$router.push('/gw/tasks')">
          <span>&larr;</span> 返回任务列表
        </button>
        <h2 class="card-title">实测 AQI 数据录入</h2>
      </div>

      <div v-if="!task" class="empty-wrap">
        <span class="empty-icon">⚠️</span>
        <span>未找到任务数据，请从任务列表进入</span>
      </div>

      <template v-else>
        <div class="task-brief">
          <div class="brief-row"><span class="brief-label">任务编号</span><span>{{ task.taskId }}</span></div>
          <div class="brief-row"><span class="brief-label">网格区域</span><span>{{ task.provinceName }} · {{ task.cityName }}</span></div>
          <div class="brief-row"><span class="brief-label">具体地址</span><span>{{ task.address }}</span></div>
          <div class="brief-row"><span class="brief-label">预估等级</span><span>{{ gradeText(task.estimatedGrade) }}</span></div>
          <div class="brief-row"><span class="brief-label">反馈描述</span><span>{{ task.information }}</span></div>
        </div>

        <form class="measure-form" @submit.prevent="handleSubmit">
          <div class="section-title">三项污染物实测浓度等级</div>

          <div class="measure-grid">
            <div class="measure-item" v-for="item in pollutants" :key="item.key">
              <label class="form-label">{{ item.label }} <span class="required">*</span></label>
              <div class="grade-btns">
                <button
                  v-for="g in gradeOptions"
                  :key="g.value"
                  type="button"
                  class="grade-btn"
                  :class="{ active: measure[item.key] === g.value, ['g' + g.value]: true }"
                  @click="measure[item.key] = g.value"
                >{{ g.value }}级</button>
              </div>
            </div>
          </div>

          <div class="aqi-result" :class="'g' + aqiGrade">
            <div class="aqi-label">
              系统按 <b>AQI = MAX（SO₂, CO, PM2.5）</b> 自动计算：
            </div>
            <div class="aqi-value">
              当前网格区域 AQI 等级：<b>{{ aqiGrade > 0 ? gradeText(aqiGrade) : '待完整录入' }}</b>
            </div>
          </div>

          <div class="form-actions">
            <button type="button" class="btn btn-default" @click="resetMeasure">重新录入</button>
            <button type="submit" class="btn btn-primary" :disabled="submitting || aqiGrade === 0">
              {{ submitting ? '提交中...' : '确认提交' }}
            </button>
          </div>
        </form>
      </template>
    </div>
  </div>
</template>

<script>
import { submitMeasure } from '../../api/task'
import { AQI_GRADES, gradeText, calcAqiGrade } from '../../constants/aqi'

export default {
  name: 'TaskMeasureView',
  data() {
    return {
      task: null,
      pollutants: [
        { key: 'so2Grade', label: 'SO₂ 二氧化硫浓度等级' },
        { key: 'coGrade', label: 'CO 一氧化碳浓度等级' },
        { key: 'pm25Grade', label: 'PM2.5 悬浮颗粒物浓度等级' }
      ],
      measure: {
        so2Grade: 0,
        coGrade: 0,
        pm25Grade: 0
      },
      gradeOptions: AQI_GRADES,
      submitting: false,
      toast: { show: false, type: 'success', message: '' },
      toastTimer: null
    }
  },
  computed: {
    aqiGrade() {
      return calcAqiGrade(this.measure.so2Grade, this.measure.coGrade, this.measure.pm25Grade)
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
    showToast(type, message) {
      this.toast = { show: true, type, message }
      if (this.toastTimer) clearTimeout(this.toastTimer)
      this.toastTimer = setTimeout(() => { this.toast.show = false }, 2500)
    },
    resetMeasure() {
      this.measure = { so2Grade: 0, coGrade: 0, pm25Grade: 0 }
    },
    async handleSubmit() {
      if (this.aqiGrade === 0) {
        this.showToast('warning', '请完整录入三项检测数据')
        return
      }
      this.submitting = true
      try {
        await submitMeasure(this.task, { ...this.measure, aqiGrade: this.aqiGrade })
        this.showToast('success', '提交成功，任务已完成，数据已进入确认AQI数据列表')
        setTimeout(() => this.$router.push('/gw/tasks'), 1200)
      } catch (err) {
        console.error(err)
        this.showToast('error', '提交失败，请重试')
        this.submitting = false
      }
    }
  }
}
</script>

<style scoped>
.page {
  padding: 28px 32px;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  text-align: left;
}

.card {
  width: 860px;
  max-width: 100%;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.card-header {
  padding: 20px 28px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: transparent;
  border: 1px solid #dcdfe6;
  color: #606266;
  padding: 7px 14px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}

.back-btn:hover {
  color: #42b983;
  border-color: #42b983;
  background: #e8f7f0;
}

.card-title {
  margin: 0;
  font-size: 19px;
  color: #2c3e50;
}

.task-brief {
  margin: 20px 28px 0;
  padding: 14px 18px;
  background: #f8fafb;
  border-radius: 10px;
}

.brief-row {
  display: flex;
  font-size: 13px;
  line-height: 1.9;
}

.brief-label {
  width: 76px;
  flex-shrink: 0;
  color: #909399;
}

.measure-form {
  padding: 20px 28px 28px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #2e8565;
  margin: 4px 0 18px;
  padding-left: 12px;
  border-left: 4px solid #42b983;
  line-height: 1;
}

.measure-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 22px;
}

.form-label {
  display: block;
  font-size: 14px;
  color: #303133;
  margin-bottom: 8px;
  font-weight: 500;
}

.required {
  color: #f56c6c;
}

.grade-btns {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.grade-btn {
  min-width: 74px;
  padding: 9px 0;
  border: 1px solid #dcdfe6;
  background: #fff;
  border-radius: 8px;
  font-size: 13px;
  color: #606266;
  cursor: pointer;
  transition: all 0.15s;
}

.grade-btn:hover {
  border-color: #42b983;
  color: #2e8565;
}

.grade-btn.active.g1 { background: #95e8a7; border-color: #95e8a7; color: #1a6b32; font-weight: 700; }
.grade-btn.active.g2 { background: #e1f3d8; border-color: #b3e19d; color: #529b2e; font-weight: 700; }
.grade-btn.active.g3 { background: #faecd8; border-color: #f3d19e; color: #b88230; font-weight: 700; }
.grade-btn.active.g4 { background: #fde2e2; border-color: #fab6b6; color: #c45656; font-weight: 700; }
.grade-btn.active.g5 { background: #f89898; border-color: #f89898; color: #fff; font-weight: 700; }
.grade-btn.active.g6 { background: #7b4a12; border-color: #7b4a12; color: #fff; font-weight: 700; }

.aqi-result {
  padding: 16px 20px;
  border-radius: 10px;
  margin-bottom: 8px;
  background: #f4f4f5;
  border: 1px solid #e4e7ed;
}

.aqi-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}

.aqi-value {
  font-size: 15px;
  color: #303133;
}

.form-actions {
  display: flex;
  justify-content: center;
  gap: 14px;
  margin-top: 22px;
  padding-top: 22px;
  border-top: 1px solid #ebeef5;
}

.btn {
  padding: 10px 30px;
  font-size: 15px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  font-weight: 500;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-primary {
  background: linear-gradient(135deg, #42b983, #2e8565);
  color: #fff;
  box-shadow: 0 4px 12px rgba(66, 185, 131, 0.3);
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(66, 185, 131, 0.4);
}

.btn-default {
  background: #fff;
  color: #606266;
  border: 1px solid #dcdfe6;
}

.btn-default:hover {
  color: #42b983;
  border-color: #42b983;
}

.empty-wrap {
  padding: 80px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  color: #c0c4cc;
  font-size: 14px;
}

.empty-icon {
  font-size: 40px;
}

/* Toast */
.toast {
  position: fixed;
  top: 24px;
  left: 50%;
  transform: translateX(-50%);
  padding: 12px 22px;
  border-radius: 8px;
  font-size: 14px;
  z-index: 9999;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
}

.toast.success { background: #f0f9eb; color: #67c23a; border: 1px solid #e1f3d8; }
.toast.error   { background: #fef0f0; color: #f56c6c; border: 1px solid #fde2e2; }
.toast.warning { background: #fdf6ec; color: #e6a23c; border: 1px solid #faecd8; }

.toast-enter-active, .toast-leave-active { transition: all 0.3s ease; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translate(-50%, -20px); }
</style>
