<template>
  <div class="auth-page">
    <!-- 左侧品牌区 -->
    <div class="brand-panel">
      <div class="brand-top">
        <div class="brand-logo"><i class="fa-solid fa-leaf"></i></div>
        <div class="brand-name">东软环保公众监督系统</div>
      </div>
      <h1 class="brand-slogan">人人都是<br />环境监督员</h1>
      <p class="brand-desc">注册成为公众监督员，随手反馈身边的空气质量，共同守护蓝天。</p>
      <div class="feature-grid">
        <div class="feature"><i class="fa-solid fa-mobile-screen"></i><span>手机号一键注册</span></div>
        <div class="feature"><i class="fa-solid fa-location-dot"></i><span>网格化地址绑定</span></div>
        <div class="feature"><i class="fa-solid fa-cloud-sun"></i><span>空气质量预估反馈</span></div>
        <div class="feature"><i class="fa-solid fa-clock-rotate-left"></i><span>历史反馈随时查</span></div>
      </div>
      <div class="brand-foot">Copyright © Neusoft Educational · 东软教育</div>
    </div>

    <!-- 右侧注册表单 -->
    <div class="form-panel">
      <div class="form-box">
        <el-button text class="back-btn" @click="$router.push('/login')">
          <i class="fa-solid fa-arrow-left"></i> 返回登录
        </el-button>
        <h2 class="form-title">注册公众监督员</h2>
        <p class="form-sub">手机号是您的身份唯一识别，请如实填写</p>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large">
          <el-form-item label="手机号" prop="telId">
            <el-input v-model.trim="form.telId" maxlength="11" placeholder="请输入11位手机号" clearable>
              <template #prefix><i class="fa-solid fa-mobile-screen-button"></i></template>
            </el-input>
          </el-form-item>
          <div class="row-2">
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model.trim="form.realName" maxlength="20" placeholder="便于工作人员联系" />
            </el-form-item>
            <el-form-item label="年龄" prop="age">
              <el-input-number v-model="form.age" :min="1" :max="120" style="width: 100%" />
            </el-form-item>
          </div>
          <el-form-item label="性别" prop="gender">
            <el-radio-group v-model="form.gender" class="gender-group">
              <el-radio-button value="男"><i class="fa-solid fa-mars"></i> 男</el-radio-button>
              <el-radio-button value="女"><i class="fa-solid fa-venus"></i> 女</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="登录密码" prop="password">
            <el-input v-model.trim="form.password" type="password" show-password placeholder="至少6位">
              <template #prefix><i class="fa-solid fa-lock"></i></template>
            </el-input>
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPwd">
            <el-input v-model.trim="form.confirmPwd" type="password" show-password placeholder="请再次输入密码">
              <template #prefix><i class="fa-solid fa-lock"></i></template>
            </el-input>
          </el-form-item>

          <el-button type="primary" size="large" class="nep-btn-gradient register-btn" :loading="loading"
            @click="handleSubmit">
            {{ loading ? '注册中...' : '注  册' }}
          </el-button>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import { register } from '../api/auth'

export default {
  name: 'RegisterView',
  data() {
    const validateTel = (rule, value, callback) => {
      if (!/^1\d{10}$/.test(value || '')) callback(new Error('请输入正确的11位手机号'))
      else callback()
    }
    return {
      form: {
        telId: '',
        password: '',
        confirmPwd: '',
        realName: '',
        age: 25,
        gender: '男'
      },
      rules: {
        telId: [{ required: true, validator: validateTel, trigger: 'blur' }],
        realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
        password: [{ required: true, min: 6, message: '密码不能少于6位', trigger: 'blur' }],
        confirmPwd: [
          { required: true, message: '请再次输入密码', trigger: 'blur' },
          {
            validator: (rule, value, callback) => {
              if (value !== this.form.password) callback(new Error('两次输入的密码不一致'))
              else callback()
            },
            trigger: 'blur'
          }
        ]
      },
      loading: false
    }
  },
  methods: {
    handleSubmit() {
      this.$refs.formRef.validate(async valid => {
        if (!valid) return
        this.loading = true
        try {
          await register({
            telId: this.form.telId,
            password: this.form.password,
            realName: this.form.realName,
            age: this.form.age,
            gender: this.form.gender
          })
          ElMessage.success('注册成功，请登录')
          this.$router.push('/login')
        } catch (err) {
          ElMessage.error(err.message || '注册失败')
        } finally {
          this.loading = false
        }
      })
    }
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
}

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
  max-width: 430px;
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
}

.feature i {
  color: #6ee7b7;
}

.brand-foot {
  margin-top: auto;
  padding-top: 40px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
}

/* ---------------- 表单 ---------------- */
.form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f6f4;
  padding: 40px 24px;
}

.form-box {
  width: 470px;
  max-width: 100%;
  background: #fff;
  border-radius: 20px;
  padding: 36px 40px 30px;
  box-shadow: 0 10px 40px rgba(15, 23, 42, 0.08);
  position: relative;
}

.back-btn {
  position: absolute;
  top: 18px;
  left: 18px;
  color: #94a3b8;
}

.form-title {
  margin: 0 0 6px;
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
}

.form-sub {
  margin: 0 0 20px;
  font-size: 13px;
  color: #94a3b8;
}

.row-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.age-tip {
  height: 40px;
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #94a3b8;
  background: #f8fafc;
  border-radius: 8px;
  padding: 0 12px;
  width: 100%;
}

.register-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  letter-spacing: 4px;
  margin-top: 4px;
}

@media (max-width: 900px) {
  .brand-panel {
    display: none;
  }
}
</style>
