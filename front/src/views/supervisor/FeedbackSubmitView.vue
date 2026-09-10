<template>
  <div class="page">
    <transition name="toast">
      <div v-if="toast.show" class="toast" :class="toast.type">
        <span class="toast-msg">{{ toast.message }}</span>
      </div>
    </transition>

    <div class="card">
      <div class="card-header">
        <h2 class="card-title">提交空气质量监督信息</h2>
        <p class="card-sub">参照《空气质量指数（AQI）范围及相应类别表》预估等级并描述观测情况</p>
      </div>

      <div v-if="!profile" class="no-profile">
        <span class="no-profile-icon">📍</span>
        <p>您还未绑定网格地址，请先完成地址绑定</p>
        <button class="btn btn-primary" @click="$router.push('/sf/address')">去绑定地址</button>
      </div>

      <form v-else class="feedback-form" @submit.prevent="handleSubmit">
        <div class="profile-bar">
          <span class="profile-item">📍 {{ profile.provinceName }} · {{ profile.cityName }}</span>
          <span class="profile-item">{{ profile.address }}</span>
          <router-link to="/sf/address" class="profile-edit">修改地址</router-link>
        </div>

        <div class="form-row">
          <div class="form-item">
            <label class="form-label">预估AQI等级 <span class="required">*</span></label>
            <select v-model="form.estimatedGrade" class="form-input" required>
              <option v-for="g in gradeOptions" :key="g.value" :value="g.value">{{ g.label }}</option>
            </select>
          </div>

          <div class="form-item">
            <label class="form-label">观测时间</label>
            <input type="datetime-local" v-model="observedAt" class="form-input">
          </div>
        </div>

        <div class="form-item">
          <label class="form-label">空气质量描述 <span class="required">*</span></label>
          <textarea
            v-model.trim="form.information"
            class="form-input form-textarea"
            rows="4"
            placeholder="请描述观测到的空气质量情况，如气味、能见度、污染源等..."
            required
          ></textarea>
        </div>

        <div class="form-actions">
          <button type="submit" class="btn btn-primary" :disabled="submitting">
            {{ submitting ? '提交中...' : '提交反馈' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script>
import { saveAqiFeedback } from '../../api/aqiFeedback'
import { AQI_GRADES } from '../../constants/aqi'

export default {
  name: 'FeedbackSubmitView',
  data() {
    return {
      form: {
        estimatedGrade: 1,
        information: ''
      },
      observedAt: '',
      gradeOptions: AQI_GRADES,
      submitting: false,
      toast: { show: false, type: 'success', message: '' },
      toastTimer: null
    }
  },
  computed: {
    profile() {
      return this.$store.state.profile
    },
    user() {
      return this.$store.state.user || {}
    }
  },
  created() {
    // 默认观测时间为当前时间
    const now = new Date()
    const pad = n => String(n).padStart(2, '0')
    this.observedAt = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}`
  },
  methods: {
    showToast(type, message) {
      this.toast = { show: true, type, message }
      if (this.toastTimer) clearTimeout(this.toastTimer)
      this.toastTimer = setTimeout(() => { this.toast.show = false }, 2500)
    },
    async handleSubmit() {
      if (!this.form.information) {
        this.showToast('warning', '请填写空气质量描述')
        return
      }
      this.submitting = true
      try {
        const [date, time] = this.observedAt ? this.observedAt.split('T') : ['', '']
        await saveAqiFeedback({
          telId: this.user.account,
          provinceId: Number(this.profile.provinceId),
          cityId: Number(this.profile.cityId),
          address: this.profile.address,
          afDate: date,
          afTime: time ? (time.length === 5 ? time + ':00' : time) : '',
          information: this.form.information,
          estimatedGrade: this.form.estimatedGrade
        })
        this.showToast('success', '提交成功，等待管理员指派网格员检测')
        this.form.information = ''
      } catch (err) {
        console.error(err)
        this.showToast('error', '提交失败，请确认后端服务已启动')
      } finally {
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
  width: 720px;
  max-width: 100%;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.card-header {
  padding: 22px 28px;
  border-bottom: 1px solid #ebeef5;
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

.no-profile {
  padding: 70px 20px;
  text-align: center;
  color: #909399;
}

.no-profile-icon {
  font-size: 44px;
  display: block;
  margin-bottom: 14px;
}

.profile-bar {
  display: flex;
  align-items: center;
  gap: 18px;
  flex-wrap: wrap;
  margin: 20px 28px 0;
  padding: 12px 16px;
  background: #f0f9f4;
  border: 1px solid #d9f0e4;
  border-radius: 8px;
  font-size: 13px;
  color: #2e8565;
}

.profile-edit {
  margin-left: auto;
  color: #42b983;
  font-size: 13px;
  text-decoration: none;
}

.profile-edit:hover {
  text-decoration: underline;
}

.feedback-form {
  padding: 20px 28px 28px;
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
  margin-top: 8px;
}

.btn {
  padding: 10px 34px;
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

@media (max-width: 640px) {
  .form-row { grid-template-columns: 1fr; }
}
</style>
