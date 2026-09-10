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
        <button class="back-btn" @click="$router.push('/aqiFeedback')">
          <span class="back-arrow">&larr;</span> 返回列表
        </button>
        <h2 class="form-title">{{ form.afId ? '修改空气质量反馈' : '新增空气质量反馈' }}</h2>
      </div>

      <div v-if="loadingData" class="loading-wrap">
        <div class="spinner"></div>
        <span class="loading-text">数据加载中...</span>
      </div>

      <form v-else class="feedback-form" @submit.prevent="handleSubmit">
        <div class="form-row">
          <div class="form-item">
            <label class="form-label">手机号 <span class="required">*</span></label>
            <input type="text" v-model="form.telId" placeholder="请输入手机号" required class="form-input">
          </div>

          <div class="form-item">
            <label class="form-label">预估等级 <span class="required">*</span></label>
            <select v-model="form.estimatedGrade" required class="form-input">
              <option :value="0">未评级</option>
              <option :value="1">一级</option>
              <option :value="2">二级</option>
              <option :value="3">三级</option>
              <option :value="4">四级</option>
              <option :value="5">五级</option>
            </select>
          </div>
        </div>

        <div class="form-row">
          <div class="form-item">
            <label class="form-label">省份ID <span class="required">*</span></label>
            <input type="number" v-model.number="form.provinceId" placeholder="请输入省份ID" required class="form-input">
          </div>

          <div class="form-item">
            <label class="form-label">城市ID <span class="required">*</span></label>
            <input type="number" v-model.number="form.cityId" placeholder="请输入城市ID" required class="form-input">
          </div>
        </div>

        <div class="form-item">
          <label class="form-label">详细地址 <span class="required">*</span></label>
          <input type="text" v-model="form.address" placeholder="请输入地址" required class="form-input">
        </div>

        <div class="form-row">
          <div class="form-item">
            <label class="form-label">日期 <span class="required">*</span></label>
            <input type="date" v-model="form.afDate" required class="form-input">
          </div>

          <div class="form-item">
            <label class="form-label">时间 <span class="required">*</span></label>
            <!-- step="1" 支持秒 -->
            <input type="time" v-model="form.afTime" step="1" required class="form-input">
          </div>
        </div>

        <div class="form-item">
          <label class="form-label">反馈信息 <span class="required">*</span></label>
          <textarea
            v-model="form.information"
            class="form-input form-textarea"
            rows="3"
            placeholder="请描述当地空气质量情况..."
            required
          ></textarea>
        </div>

        <div class="form-actions">
          <button type="button" class="btn btn-default" @click="$router.back()">取 消</button>
          <button type="submit" class="btn btn-primary">{{ form.afId ? '保存修改' : '立即提交' }}</button>
        </div>
      </form>
    </div>
  </div>
</template>

<script>
import { saveAqiFeedback, getAqiFeedbackById, updateAqiFeedback } from '../api/aqiFeedback.js'

export default {
  name: 'AqiFeedBackForm',
  data() {
    return {
      form: {
        afId: null,
        telId: '',
        provinceId: 1,
        cityId: 1,
        address: '',
        afDate: '',
        afTime: '',
        information: '',
        estimatedGrade: 0
      },
      loadingData: false,
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
    const afId = this.$route.query.afId
    if (afId) {
      this.loadingData = true
      try {
        await this.loadDetail(afId)
      } catch (err) {
        console.error('加载详情失败', err)
        this.showToast('error', '加载数据失败，请返回重试')
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
    async loadDetail(afId) {
      const res = await getAqiFeedbackById(afId)
      if (res.data && res.data.data) {
        const detail = res.data.data
        this.form = { ...this.form, ...detail } // 使用对象展开运算符简化赋值
      }
    },
    async handleSubmit() {
      try {
        let res
        if (this.form.afId) {
          res = await updateAqiFeedback(this.form)
        } else {
          res = await saveAqiFeedback(this.form)
        }

        // 后端返回 code 200 为成功
        if (res.data.code === 200) {
          this.showToast('success', this.form.afId ? '修改成功' : '新增成功')
          setTimeout(() => {
            this.$router.push('/aqiFeedback')
          }, 800)
        } else {
          this.showToast('error', res.data.msg || '操作失败')
        }
      } catch (err) {
        console.error(err)
        this.showToast('error', '提交失败，请检查网络或后端接口')
      }
    }
  }
}
</script>

<style scoped>
.form-page {
  padding: 28px 32px;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  text-align: left;
}

.form-card {
  width: 760px;
  max-width: 100%;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.form-header {
  padding: 22px 28px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  gap: 16px;
}

.form-title {
  margin: 0;
  font-size: 19px;
  color: #2c3e50;
  flex: 1;
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

.back-arrow {
  font-size: 15px;
}

.feedback-form {
  padding: 28px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}

.form-item {
  display: flex;
  flex-direction: column;
  margin-bottom: 18px;
}

.form-label {
  font-size: 14px;
  color: #303133;
  margin-bottom: 7px;
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
  border-radius: 8px;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
  background: #fff;
  color: #303133;
}

.form-input:focus {
  border-color: #42b983;
  box-shadow: 0 0 0 3px rgba(66, 185, 131, 0.15);
}

.form-textarea {
  resize: vertical;
  font-family: inherit;
  line-height: 1.6;
}

.form-actions {
  display: flex;
  justify-content: center;
  gap: 14px;
  margin-top: 10px;
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

.btn-primary {
  background: linear-gradient(135deg, #42b983, #2e8565);
  color: #fff;
  box-shadow: 0 4px 12px rgba(66, 185, 131, 0.3);
}

.btn-primary:hover {
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

/* 加载态 */
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

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Toast 提示 */
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
}
</style>
