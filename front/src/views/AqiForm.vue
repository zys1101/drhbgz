<template>
  <div class="form-page">
    <transition name="toast">
      <div v-if="toast.show" class="toast" :class="toast.type">
        <span class="toast-icon">{{ toastIcon }}</span>
        <span class="toast-msg">{{ toast.message }}</span>
      </div>
    </transition>

    <div class="form-card">
      <div class="form-header">
        <button class="back-btn" @click="goBack">
          <span class="back-arrow">&larr;</span> 返回列表
        </button>
        <h2 class="form-title">{{ form.aqiId ? '修改空气质量指数级别' : '新增空气质量指数级别' }}</h2>
      </div>

      <div v-if="loadingData" class="loading-wrap">
        <div class="spinner"></div>
        <span class="loading-text">数据加载中...</span>
      </div>

      <form v-else class="aqi-form" @submit.prevent="handleSubmit">
        <div class="form-row">
          <div class="form-item">
            <label class="form-label">级别汉字表述 <span class="required">*</span></label>
            <input
              type="text"
              v-model="form.chineseExplain"
              class="form-input"
              :class="{ 'has-error': errors.chineseExplain }"
              placeholder="如：一级"
            >
            <span v-if="errors.chineseExplain" class="error-msg">{{ errors.chineseExplain }}</span>
          </div>

          <div class="form-item">
            <label class="form-label">级别描述 <span class="required">*</span></label>
            <input
              type="text"
              v-model="form.aqiExplain"
              class="form-input"
              :class="{ 'has-error': errors.aqiExplain }"
              placeholder="如：优"
            >
            <span v-if="errors.aqiExplain" class="error-msg">{{ errors.aqiExplain }}</span>
          </div>
        </div>

        <div class="form-row">
          <div class="form-item">
            <label class="form-label">级别颜色 <span class="required">*</span></label>
            <div class="color-picker-wrap">
              <input
                type="color"
                v-model="form.color"
                class="color-picker"
              >
              <input
                type="text"
                v-model="form.color"
                class="form-input color-text"
                :class="{ 'has-error': errors.color }"
                placeholder="#000000"
                maxlength="7"
              >
            </div>
            <span v-if="errors.color" class="error-msg">{{ errors.color }}</span>
          </div>

          <div class="form-item">
            <label class="form-label">备注</label>
            <input
              type="text"
              v-model="form.remarks"
              class="form-input"
              placeholder="选填"
            >
          </div>
        </div>

        <div class="section-title">污染物浓度限值</div>

        <div class="form-row">
          <div class="form-item">
            <label class="form-label">二氧化硫(SO₂) 最小值 <span class="required">*</span></label>
            <input
              type="number"
              v-model.number="form.so2Min"
              class="form-input"
              :class="{ 'has-error': errors.so2Min }"
              placeholder="0"
            >
            <span v-if="errors.so2Min" class="error-msg">{{ errors.so2Min }}</span>
          </div>

          <div class="form-item">
            <label class="form-label">二氧化硫(SO₂) 最大值 <span class="required">*</span></label>
            <input
              type="number"
              v-model.number="form.so2Max"
              class="form-input"
              :class="{ 'has-error': errors.so2Max }"
              placeholder="0"
            >
            <span v-if="errors.so2Max" class="error-msg">{{ errors.so2Max }}</span>
          </div>
        </div>

        <div class="form-row">
          <div class="form-item">
            <label class="form-label">一氧化碳(CO) 最小值 <span class="required">*</span></label>
            <input
              type="number"
              v-model.number="form.coMin"
              class="form-input"
              :class="{ 'has-error': errors.coMin }"
              placeholder="0"
            >
            <span v-if="errors.coMin" class="error-msg">{{ errors.coMin }}</span>
          </div>

          <div class="form-item">
            <label class="form-label">一氧化碳(CO) 最大值 <span class="required">*</span></label>
            <input
              type="number"
              v-model.number="form.coMax"
              class="form-input"
              :class="{ 'has-error': errors.coMax }"
              placeholder="0"
            >
            <span v-if="errors.coMax" class="error-msg">{{ errors.coMax }}</span>
          </div>
        </div>

        <div class="form-row">
          <div class="form-item">
            <label class="form-label">悬浮颗粒物(SPM) 最小值 <span class="required">*</span></label>
            <input
              type="number"
              v-model.number="form.spmMin"
              class="form-input"
              :class="{ 'has-error': errors.spmMin }"
              placeholder="0"
            >
            <span v-if="errors.spmMin" class="error-msg">{{ errors.spmMin }}</span>
          </div>

          <div class="form-item">
            <label class="form-label">悬浮颗粒物(SPM) 最大值 <span class="required">*</span></label>
            <input
              type="number"
              v-model.number="form.spmMax"
              class="form-input"
              :class="{ 'has-error': errors.spmMax }"
              placeholder="0"
            >
            <span v-if="errors.spmMax" class="error-msg">{{ errors.spmMax }}</span>
          </div>
        </div>

        <div class="form-item full-width">
          <label class="form-label">对健康影响情况 <span class="required">*</span></label>
          <textarea
            v-model="form.healthImpact"
            class="form-input form-textarea"
            :class="{ 'has-error': errors.healthImpact }"
            placeholder="请描述该级别对健康的影响..."
            rows="3"
          ></textarea>
          <span v-if="errors.healthImpact" class="error-msg">{{ errors.healthImpact }}</span>
        </div>

        <div class="form-item full-width">
          <label class="form-label">建议采取的措施 <span class="required">*</span></label>
          <textarea
            v-model="form.takeSteps"
            class="form-input form-textarea"
            :class="{ 'has-error': errors.takeSteps }"
            placeholder="请描述该级别建议采取的防护措施..."
            rows="3"
          ></textarea>
          <span v-if="errors.takeSteps" class="error-msg">{{ errors.takeSteps }}</span>
        </div>

        <div class="form-actions">
          <button type="button" class="btn btn-default" @click="resetForm">重 置</button>
          <button type="submit" class="btn btn-primary" :disabled="submitting">
            <span v-if="submitting" class="btn-spinner"></span>
            {{ submitting ? '提交中...' : (form.aqiId ? '修 改' : '提 交') }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script>
import { saveAqi, getAqiById, updateAqi } from '../api/aqi.js'

export default {
  name: 'AqiForm',
  data() {
    return {
      form: {
        aqiId: null,
        chineseExplain: '',
        aqiExplain: '',
        color: '#000000',
        healthImpact: '',
        takeSteps: '',
        so2Min: 0,
        so2Max: 0,
        coMin: 0,
        coMax: 0,
        spmMin: 0,
        spmMax: 0,
        remarks: ''
      },
      errors: {},
      loadingData: false,
      submitting: false,
      toast: {
        show: false,
        type: 'success',
        message: ''
      },
      toastTimer: null
    }
  },
  computed: {
    toastIcon() {
      const map = { success: '✓', error: '✕', warning: '!', info: 'i' }
      return map[this.toast.type] || 'i'
    }
  },
  async mounted() {
    const aqiId = this.$route.query.aqiId
    if (aqiId) {
      this.loadingData = true
      try {
        this.form.aqiId = aqiId
        await this.loadDetail(aqiId)
      } catch (err) {
        this.showToast('error', '数据加载失败，请刷新重试')
      } finally {
        this.loadingData = false
      }
    }
  },
  methods: {
    showToast(type, message) {
      this.toast = { show: true, type, message }
      if (this.toastTimer) clearTimeout(this.toastTimer)
      this.toastTimer = setTimeout(() => {
        this.toast.show = false
      }, 2500)
    },

    async loadDetail(aqiId) {
      const res = await getAqiById(aqiId)
      const detail = res.data.data
      Object.assign(this.form, {
        aqiId: detail.aqiId,
        chineseExplain: detail.chineseExplain,
        aqiExplain: detail.aqiExplain,
        color: detail.color || '#000000',
        healthImpact: detail.healthImpact,
        takeSteps: detail.takeSteps,
        so2Min: detail.so2Min,
        so2Max: detail.so2Max,
        coMin: detail.coMin,
        coMax: detail.coMax,
        spmMin: detail.spmMin,
        spmMax: detail.spmMax,
        remarks: detail.remarks
      })
    },

    validate() {
      const errors = {}
      if (!this.form.chineseExplain) errors.chineseExplain = '请输入级别汉字表述'
      if (!this.form.aqiExplain) errors.aqiExplain = '请输入级别描述'
      if (!this.form.color) {
        errors.color = '请选择级别颜色'
      } else if (!/^#([0-9a-fA-F]{6})$/.test(this.form.color)) {
        errors.color = '颜色格式不正确，应为 #RRGGBB'
      }
      if (this.form.so2Min === '' || this.form.so2Min === null || isNaN(this.form.so2Min)) errors.so2Min = '请输入SO₂最小值'
      if (this.form.so2Max === '' || this.form.so2Max === null || isNaN(this.form.so2Max)) errors.so2Max = '请输入SO₂最大值'
      if (this.form.coMin === '' || this.form.coMin === null || isNaN(this.form.coMin)) errors.coMin = '请输入CO最小值'
      if (this.form.coMax === '' || this.form.coMax === null || isNaN(this.form.coMax)) errors.coMax = '请输入CO最大值'
      if (this.form.spmMin === '' || this.form.spmMin === null || isNaN(this.form.spmMin)) errors.spmMin = '请输入SPM最小值'
      if (this.form.spmMax === '' || this.form.spmMax === null || isNaN(this.form.spmMax)) errors.spmMax = '请输入SPM最大值'
      if (!this.form.healthImpact) errors.healthImpact = '请输入对健康影响情况'
      if (!this.form.takeSteps) errors.takeSteps = '请输入建议采取的措施'
      this.errors = errors
      return Object.keys(errors).length === 0
    },

    resetForm() {
      const aqiId = this.form.aqiId
      this.form = {
        aqiId: aqiId,
        chineseExplain: '',
        aqiExplain: '',
        color: '#000000',
        healthImpact: '',
        takeSteps: '',
        so2Min: 0,
        so2Max: 0,
        coMin: 0,
        coMax: 0,
        spmMin: 0,
        spmMax: 0,
        remarks: ''
      }
      this.errors = {}
    },

    goBack() {
      this.$router.push('/aqi')
    },

    async handleSubmit() {
      if (!this.validate()) {
        this.showToast('warning', '请完善表单信息')
        return
      }
      this.submitting = true
      try {
        let res
        if (this.form.aqiId) {
          res = await updateAqi(this.form)
          if (res.data.code === 200) {
            this.showToast('success', '修改成功')
          } else {
            this.showToast('error', res.data.message || '修改失败')
            this.submitting = false
            return
          }
        } else {
          res = await saveAqi(this.form)
          if (res.data.code === 200) {
            this.showToast('success', '新增成功')
          } else {
            this.showToast('error', res.data.message || '新增失败')
            this.submitting = false
            return
          }
        }
        setTimeout(() => {
          this.$router.push('/aqi')
        }, 800)
      } catch (err) {
        console.log(err)
        this.showToast('error', '网络异常，请稍后重试')
        this.submitting = false
      }
    }
  }
}
</script>

<style scoped>
.form-page {
  min-height: 100vh;
  padding: 28px 32px;
  box-sizing: border-box;
  text-align: left;
}

.form-card {
  max-width: 960px;
  margin: 0 auto;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  overflow: hidden;
}

.form-header {
  padding: 24px 32px;
  border-bottom: 1px solid #eee;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fafbfc;
}

.form-title {
  margin: 0;
  font-size: 20px;
  color: #2c3e50;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: transparent;
  border: 1px solid #dcdfe6;
  color: #606266;
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.back-btn:hover {
  color: #42b983;
  border-color: #42b983;
  background: #e8f7f0;
}

.back-arrow {
  font-size: 16px;
}

.aqi-form {
  padding: 32px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.form-item {
  display: flex;
  flex-direction: column;
}

.form-item.full-width {
  margin-bottom: 20px;
}

.form-label {
  font-size: 14px;
  color: #333;
  margin-bottom: 8px;
  font-weight: 500;
}

.required {
  color: #f56c6c;
}

.form-input {
  width: 100%;
  padding: 10px 14px;
  font-size: 14px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
  background: #fff;
  box-sizing: border-box;
  color: #333;
}

.form-input:focus {
  border-color: #42b983;
  box-shadow: 0 0 0 3px rgba(66, 185, 131, 0.15);
}

.form-input.has-error {
  border-color: #f56c6c;
}

.form-textarea {
  resize: vertical;
  font-family: inherit;
}

.error-msg {
  color: #f56c6c;
  font-size: 12px;
  margin-top: 6px;
  line-height: 1.2;
}

.color-picker-wrap {
  display: flex;
  gap: 12px;
  align-items: center;
}

.color-picker {
  width: 48px;
  height: 40px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
  padding: 2px;
  flex-shrink: 0;
}

.color-text {
  flex: 1;
  font-family: monospace;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #42b983;
  margin: 8px 0 20px;
  padding-left: 12px;
  border-left: 4px solid #42b983;
  line-height: 1;
}

.form-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 12px;
  padding-top: 24px;
  border-top: 1px solid #eee;
}

.btn {
  padding: 10px 32px;
  font-size: 15px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 8px;
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

.btn-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  gap: 16px;
}

.spinner {
  width: 36px;
  height: 36px;
  border: 3px solid #e4e7ed;
  border-top-color: #42b983;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.loading-text {
  color: #909399;
  font-size: 14px;
}

.toast {
  position: fixed;
  top: 24px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 22px;
  border-radius: 8px;
  font-size: 14px;
  z-index: 9999;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
  max-width: 90%;
  white-space: nowrap;
}

.toast.success { background: #f0f9eb; color: #67c23a; border: 1px solid #e1f3d8; }
.toast.error   { background: #fef0f0; color: #f56c6c; border: 1px solid #fde2e2; }
.toast.warning { background: #fdf6ec; color: #e6a23c; border: 1px solid #faecd8; }
.toast.info    { background: #edf2fc; color: #909399; border: 1px solid #ebeef5; }

.toast-icon {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: currentColor;
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
}

.toast-enter-active, .toast-leave-active {
  transition: all 0.3s ease;
}

.toast-enter-from {
  opacity: 0;
  transform: translate(-50%, -20px);
}

.toast-leave-to {
  opacity: 0;
  transform: translate(-50%, -20px);
}

@media (max-width: 640px) {
  .form-row {
    grid-template-columns: 1fr;
  }
  .form-card {
    border-radius: 0;
  }
  .form-page {
    padding: 0;
  }
}
</style>