<template>
  <div class="register-page">
    <div class="register-card">
      <button class="back-btn" @click="$router.push('/login')">&larr; 返回登录</button>
      <h2 class="register-title">公众监督员注册</h2>
      <p class="register-subtitle">注册后即可提交空气质量监督信息</p>

      <form @submit.prevent="handleSubmit" class="register-form">
        <div class="form-item">
          <label class="form-label">手机号 <span class="required">*</span></label>
          <input
            type="text"
            v-model.trim="form.telId"
            class="form-input"
            :class="{ 'has-error': errors.telId }"
            placeholder="请输入11位手机号（作为唯一身份识别）"
            maxlength="11"
          >
          <span v-if="errors.telId" class="error-msg">{{ errors.telId }}</span>
        </div>

        <div class="form-item">
          <label class="form-label">登录密码 <span class="required">*</span></label>
          <input
            type="password"
            v-model.trim="form.password"
            class="form-input"
            :class="{ 'has-error': errors.password }"
            placeholder="请输入密码（至少6位）"
          >
          <span v-if="errors.password" class="error-msg">{{ errors.password }}</span>
        </div>

        <div class="form-item">
          <label class="form-label">确认密码 <span class="required">*</span></label>
          <input
            type="password"
            v-model.trim="form.confirmPwd"
            class="form-input"
            :class="{ 'has-error': errors.confirmPwd }"
            placeholder="请再次输入密码"
          >
          <span v-if="errors.confirmPwd" class="error-msg">{{ errors.confirmPwd }}</span>
        </div>

        <div class="form-item">
          <label class="form-label">真实姓名 <span class="required">*</span></label>
          <input
            type="text"
            v-model.trim="form.realName"
            class="form-input"
            :class="{ 'has-error': errors.realName }"
            placeholder="便于工作人员与您联系"
            maxlength="20"
          >
          <span v-if="errors.realName" class="error-msg">{{ errors.realName }}</span>
        </div>

        <div class="form-row">
          <div class="form-item">
            <label class="form-label">年龄 <span class="required">*</span></label>
            <input
              type="number"
              v-model.number="form.age"
              class="form-input"
              :class="{ 'has-error': errors.age }"
              placeholder="请输入年龄"
              min="1"
              max="120"
            >
            <span v-if="errors.age" class="error-msg">{{ errors.age }}</span>
          </div>

          <div class="form-item">
            <label class="form-label">性别 <span class="required">*</span></label>
            <select v-model="form.gender" class="form-input">
              <option value="男">男</option>
              <option value="女">女</option>
            </select>
          </div>
        </div>

        <p v-if="errorMsg" class="error-msg form-error">{{ errorMsg }}</p>

        <button type="submit" class="submit-btn" :disabled="loading">
          {{ loading ? '注册中...' : '注 册' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script>
import { register } from '../api/auth'

export default {
  name: 'RegisterView',
  data() {
    return {
      form: {
        telId: '',
        password: '',
        confirmPwd: '',
        realName: '',
        age: null,
        gender: '男'
      },
      errors: {},
      errorMsg: '',
      loading: false
    }
  },
  methods: {
    validate() {
      const errors = {}
      if (!/^1\d{10}$/.test(this.form.telId)) {
        errors.telId = '请输入正确的11位手机号'
      }
      if (!this.form.password || this.form.password.length < 6) {
        errors.password = '密码不能少于6位'
      }
      if (this.form.password !== this.form.confirmPwd) {
        errors.confirmPwd = '两次输入的密码不一致'
      }
      if (!this.form.realName) {
        errors.realName = '请输入真实姓名'
      }
      if (!this.form.age || this.form.age < 1 || this.form.age > 120) {
        errors.age = '请输入有效年龄'
      }
      this.errors = errors
      return Object.keys(errors).length === 0
    },
    async handleSubmit() {
      if (!this.validate()) return
      this.loading = true
      this.errorMsg = ''
      try {
        // TODO: 后端就绪后替换为真实注册接口
        await register({
          telId: this.form.telId,
          password: this.form.password,
          realName: this.form.realName,
          age: this.form.age,
          gender: this.form.gender
        })
        alert('注册成功，请登录')
        this.$router.push('/login')
      } catch (err) {
        this.errorMsg = err.message || '注册失败'
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #42b983 0%, #2c3e50 100%);
  padding: 20px;
}

.register-card {
  width: 460px;
  max-width: 100%;
  background: #fff;
  border-radius: 12px;
  padding: 32px 36px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.25);
  position: relative;
}

.back-btn {
  position: absolute;
  top: 18px;
  left: 18px;
  background: transparent;
  border: none;
  color: #909399;
  font-size: 13px;
  cursor: pointer;
}

.back-btn:hover {
  color: #42b983;
}

.register-title {
  margin: 6px 0 4px;
  font-size: 21px;
  color: #2c3e50;
  text-align: center;
}

.register-subtitle {
  margin: 0 0 22px;
  font-size: 12px;
  color: #a8abb2;
  text-align: center;
}

.form-item {
  margin-bottom: 16px;
  text-align: left;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.form-label {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  color: #606266;
}

.required {
  color: #f56c6c;
}

.form-input {
  width: 100%;
  box-sizing: border-box;
  height: 38px;
  padding: 0 12px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}

.form-input:focus {
  border-color: #42b983;
}

.form-input.has-error {
  border-color: #f56c6c;
}

.error-msg {
  display: block;
  margin-top: 5px;
  font-size: 12px;
  color: #f56c6c;
  text-align: left;
}

.form-error {
  margin: 0 0 10px;
}

.submit-btn {
  width: 100%;
  height: 40px;
  margin-top: 6px;
  border: none;
  border-radius: 6px;
  background: #42b983;
  color: #fff;
  font-size: 15px;
  cursor: pointer;
  transition: background 0.2s;
}

.submit-btn:hover:not(:disabled) {
  background: #3aa876;
}

.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
</style>
