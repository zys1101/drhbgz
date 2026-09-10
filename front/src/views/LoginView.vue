<template>
  <div class="login-page">
    <div class="login-card">
      <h2 class="login-title">东软环保公众监督系统</h2>
      <p class="login-subtitle">Environmental Public Supervision System</p>

      <div class="role-tabs">
        <button
          v-for="r in roleOptions"
          :key="r.key"
          type="button"
          class="role-tab"
          :class="{ active: role === r.key }"
          @click="switchRole(r.key)"
        >{{ r.label }}</button>
      </div>

      <form @submit.prevent="handleLogin" class="login-form">
        <div class="form-item">
          <label class="form-label">{{ isSupervisor ? '手机号' : '登录编码' }}</label>
          <input
            type="text"
            v-model.trim="account"
            class="form-input"
            :placeholder="isSupervisor ? '请输入手机号' : '请输入登录编码'"
            required
          >
        </div>

        <div class="form-item">
          <label class="form-label">密码</label>
          <input
            type="password"
            v-model.trim="password"
            class="form-input"
            placeholder="请输入密码"
            required
          >
        </div>

        <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>

        <button type="submit" class="login-btn" :disabled="loading">
          {{ loading ? '登录中...' : '登 录' }}
        </button>
      </form>

      <p class="login-tip" v-if="isSupervisor">
        还没有监督员身份？<a class="register-link" @click="$router.push('/register')">立即注册</a>
      </p>
      <p class="login-tip">{{ roleHint }}</p>
    </div>
  </div>
</template>

<script>
import { login } from '../api/auth'
import { ROLES } from '../constants/aqi'

export default {
  name: 'LoginView',
  data() {
    return {
      role: 'supervisor',
      account: '',
      password: '',
      errorMsg: '',
      loading: false,
      roleOptions: [
        { key: 'supervisor', label: '公众监督员' },
        { key: 'grid', label: '网格员' },
        { key: 'admin', label: '管理员' },
        { key: 'viewer', label: '决策者' }
      ]
    }
  },
  computed: {
    isSupervisor() {
      return this.role === 'supervisor'
    },
    roleHint() {
      const hints = {
        supervisor: '演示监督员：13800001111 / 123456（或自行注册）',
        grid: '演示网格员：grid001 / 123456（账号由HR系统同步）',
        admin: '演示管理员：admin / 123456（账号由HR系统同步）',
        viewer: '演示决策者：viewer / 123456'
      }
      return hints[this.role] || ''
    }
  },
  methods: {
    switchRole(key) {
      this.role = key
      this.errorMsg = ''
    },
    async handleLogin() {
      if (!this.account || !this.password) {
        this.errorMsg = '请输入账号和密码'
        return
      }
      this.loading = true
      this.errorMsg = ''
      try {
        // TODO: 后端就绪后替换为真实登录接口
        const user = await login(this.account, this.password, this.role)
        this.$store.dispatch('login', user)
        const redirect = this.$route.query.redirect
        this.$router.push(redirect || ROLES[user.role].home)
      } catch (err) {
        this.errorMsg = err.message || '登录失败'
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #42b983 0%, #2c3e50 100%);
  padding: 20px;
}

.login-card {
  width: 400px;
  background: #fff;
  border-radius: 12px;
  padding: 36px 36px 28px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.25);
}

.login-title {
  margin: 0 0 6px;
  font-size: 22px;
  color: #2c3e50;
  text-align: center;
}

.login-subtitle {
  margin: 0 0 22px;
  font-size: 12px;
  color: #a8abb2;
  text-align: center;
  letter-spacing: 0.5px;
}

.role-tabs {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
  margin-bottom: 22px;
  background: #f4f6f8;
  padding: 5px;
  border-radius: 9px;
}

.role-tab {
  padding: 8px 0;
  border: none;
  background: transparent;
  border-radius: 6px;
  font-size: 13px;
  color: #606266;
  cursor: pointer;
  transition: all 0.2s;
}

.role-tab.active {
  background: #fff;
  color: #2e8565;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.form-item {
  margin-bottom: 18px;
  text-align: left;
}

.form-label {
  display: block;
  margin-bottom: 6px;
  font-size: 14px;
  color: #606266;
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

.error-msg {
  margin: 0 0 12px;
  font-size: 13px;
  color: #f56c6c;
  text-align: left;
}

.login-btn {
  width: 100%;
  height: 40px;
  border: none;
  border-radius: 6px;
  background: #42b983;
  color: #fff;
  font-size: 15px;
  cursor: pointer;
  transition: background 0.2s;
}

.login-btn:hover:not(:disabled) {
  background: #3aa876;
}

.login-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.login-tip {
  margin: 16px 0 0;
  font-size: 12px;
  color: #c0c4cc;
  text-align: center;
}

.register-link {
  color: #42b983;
  cursor: pointer;
  font-weight: 600;
}
</style>
