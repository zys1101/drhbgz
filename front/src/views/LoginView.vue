<template>
  <div class="auth-page">
    <!-- 左侧品牌区 -->
    <div class="brand-panel">
      <div class="brand-top">
        <div class="brand-logo"><i class="fa-solid fa-leaf"></i></div>
        <div class="brand-name">东软环保公众监督系统</div>
        <div class="brand-en">NEUSOFT ENVIRONMENTAL PUBLIC SUPERVISION</div>
      </div>

      <h1 class="brand-slogan">守护每一口<br />新鲜空气</h1>
      <p class="brand-desc">
        汇总公众监督反馈 · 网格员实地检测 · AQI 数据统计分析 · 决策可视化大屏
      </p>

      <div class="feature-grid">
        <div class="feature"><i class="fa-solid fa-users"></i><span>公众监督员反馈</span></div>
        <div class="feature"><i class="fa-solid fa-person-walking-dashed-line-arrow-right"></i><span>网格员实地检测</span></div>
        <div class="feature"><i class="fa-solid fa-chart-column"></i><span>五项统计报表</span></div>
        <div class="feature"><i class="fa-solid fa-tv"></i><span>可视化决策大屏</span></div>
      </div>

      <div class="brand-foot">Copyright © Neusoft Educational · 东软教育</div>
    </div>

    <!-- 右侧登录表单 -->
    <div class="form-panel">
      <div class="form-box">
        <h2 class="form-title">欢迎登录</h2>
        <p class="form-sub">请选择您的用户类型并登录系统</p>

        <div class="role-tabs">
          <button
            v-for="r in roleOptions"
            :key="r.key"
            type="button"
            class="role-tab"
            :class="{ active: role === r.key }"
            @click="switchRole(r.key)"
          >
            <i :class="r.icon"></i>{{ r.label }}
          </button>
        </div>

        <el-form ref="formRef" :model="form" size="large" @submit.prevent>
          <el-form-item>
            <el-input v-model.trim="form.account" :placeholder="isSupervisor ? '请输入手机号' : '请输入登录编码'" clearable>
              <template #prefix><i class="fa-solid fa-user"></i></template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-input v-model.trim="form.password" type="password" placeholder="请输入密码" show-password
              @keyup.enter="handleLogin">
              <template #prefix><i class="fa-solid fa-lock"></i></template>
            </el-input>
          </el-form-item>
          <el-button type="primary" size="large" class="nep-btn-gradient login-btn" :loading="loading"
            @click="handleLogin">
            {{ loading ? '登录中...' : '登  录' }}
          </el-button>
        </el-form>

        <div class="form-foot">
          <template v-if="isSupervisor">
            还没有监督员身份？<el-link type="primary" :underline="false" @click="$router.push('/register')">立即注册</el-link>
          </template>
        </div>

        <el-alert v-if="roleHint" :title="roleHint" type="info" :closable="false" class="role-hint" />
      </div>
    </div>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import { login } from '../api/auth'
import { ROLES } from '../constants/aqi'

export default {
  name: 'LoginView',
  data() {
    return {
      role: 'supervisor',
      form: { account: '', password: '' },
      loading: false,
      roleOptions: [
        { key: 'supervisor', label: '公众监督员', icon: 'fa-solid fa-users' },
        { key: 'grid', label: '网格员', icon: 'fa-solid fa-helmet-safety' },
        { key: 'admin', label: '管理员', icon: 'fa-solid fa-user-gear' },
        { key: 'viewer', label: '决策者', icon: 'fa-solid fa-user-tie' }
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
    },
    async handleLogin() {
      if (!this.form.account || !this.form.password) {
        ElMessage.warning('请输入账号和密码')
        return
      }
      this.loading = true
      try {
        const user = await login(this.form.account, this.form.password, this.role)
        this.$store.dispatch('login', user)
        ElMessage.success('登录成功，欢迎回来，' + user.username)
        const redirect = this.$route.query.redirect
        this.$router.push(redirect || ROLES[user.role].home)
      } catch (err) {
        ElMessage.error(err.message || '登录失败')
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
}

/* ---------------- 左侧品牌区 ---------------- */
.brand-panel {
  flex: 1.1;
  min-width: 0;
  background:
    radial-gradient(700px 420px at 85% -10%, rgba(52, 211, 153, 0.35), transparent 60%),
    radial-gradient(600px 400px at -10% 110%, rgba(13, 148, 136, 0.4), transparent 55%),
    linear-gradient(160deg, #052e22 0%, #064e3b 60%, #065f46 100%);
  color: #fff;
  padding: 56px 64px;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

.brand-top {
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand-logo {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: linear-gradient(135deg, #34d399, #059669);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 21px;
  box-shadow: 0 6px 18px rgba(16, 185, 129, 0.5);
}

.brand-name {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 1px;
}

.brand-en {
  width: 100%;
  margin-top: 6px;
  font-size: 10.5px;
  letter-spacing: 2px;
  color: rgba(255, 255, 255, 0.45);
}

.brand-slogan {
  margin: auto 0 0;
  font-size: 46px;
  line-height: 1.3;
  font-weight: 800;
  letter-spacing: 2px;
}

.brand-desc {
  margin: 18px 0 0;
  font-size: 14.5px;
  color: rgba(255, 255, 255, 0.75);
  letter-spacing: 0.5px;
}

.feature-grid {
  margin-top: 34px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  max-width: 460px;
}

.feature {
  display: flex;
  align-items: center;
  gap: 10px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 12px;
  padding: 12px 16px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(4px);
}

.feature i {
  color: #6ee7b7;
  font-size: 15px;
}

.brand-foot {
  margin-top: auto;
  padding-top: 40px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
  letter-spacing: 0.5px;
}

/* ---------------- 右侧表单区 ---------------- */
.form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f6f4;
  padding: 40px 24px;
}

.form-box {
  width: 420px;
  max-width: 100%;
  background: #fff;
  border-radius: 20px;
  padding: 40px 40px 30px;
  box-shadow: 0 10px 40px rgba(15, 23, 42, 0.08);
}

.form-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
}

.form-sub {
  margin: 8px 0 24px;
  font-size: 13px;
  color: #94a3b8;
}

.role-tabs {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
  background: #f1f5f4;
  padding: 5px;
  border-radius: 12px;
  margin-bottom: 26px;
}

.role-tab {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  padding: 9px 0 7px;
  border: none;
  background: transparent;
  border-radius: 9px;
  font-size: 12px;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
}

.role-tab i {
  font-size: 14px;
}

.role-tab.active {
  background: #fff;
  color: #047857;
  font-weight: 600;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.08);
}

.login-btn {
  width: 100%;
  margin-top: 4px;
  height: 44px;
  font-size: 15px;
  letter-spacing: 4px;
}

.form-foot {
  margin-top: 18px;
  text-align: center;
  font-size: 13px;
  color: #94a3b8;
  min-height: 20px;
}

.role-hint {
  margin-top: 14px;
}

.role-hint :deep(.el-alert__title) {
  font-size: 12px;
}

@media (max-width: 900px) {
  .brand-panel {
    display: none;
  }
}
</style>
