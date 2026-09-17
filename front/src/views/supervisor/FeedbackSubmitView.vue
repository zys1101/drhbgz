<template>
  <div class="app-page">
    <div class="app-card">
      <div class="app-card-head">
        <div>
          <h2 class="app-card-title">
            <span class="app-ico"><i class="fa-solid fa-pen-to-square"></i></span>
            提交监督反馈
          </h2>
          <p class="app-card-sub">按您的实际观测填写，提交后管理员会指派网格员实地检测</p>
        </div>
      </div>

      <!-- 未绑定地址引导 -->
      <div v-if="!profile" class="app-empty">
        <span class="app-empty-icon"><i class="fa-solid fa-map-location-dot"></i></span>
        <p>您还未绑定网格地址，先完成绑定才能提交反馈</p>
        <button class="app-btn-primary app-btn-sm" @click="$router.push('/sf/address')">
          <i class="fa-solid fa-location-dot"></i>去绑定地址
        </button>
      </div>

      <template v-else>
        <!-- 已绑定地址条 -->
        <div class="app-profile-bar">
          <div class="profile-pin"><i class="fa-solid fa-location-dot"></i></div>
          <div class="profile-main">
            <div class="profile-region">{{ profile.provinceName }} · {{ profile.cityName }}</div>
            <div class="profile-addr">{{ profile.address }}</div>
          </div>
          <button class="profile-edit" @click="$router.push('/sf/address')">
            <i class="fa-solid fa-pen"></i> 修改
          </button>
        </div>

        <div class="app-field">
          <label class="app-field-label">预估空气等级</label>
          <el-select v-model="form.estimatedGrade" size="large" style="width: 100%">
            <el-option v-for="g in gradeOptions" :key="g.value" :value="g.value">
              <span class="grade-option">
                <span class="grade-option-dot" :style="{ background: levelColor(g.value) }"></span>
                {{ g.label }}
              </span>
            </el-option>
          </el-select>
        </div>

        <div class="app-field">
          <label class="app-field-label">观测时间</label>
          <el-date-picker v-model="observedAt" type="datetime" style="width: 100%"
            format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DD HH:mm" :clearable="false" />
        </div>

        <div class="app-field">
          <label class="app-field-label">空气质量描述</label>
          <el-input v-model.trim="form.information" type="textarea" :rows="5" maxlength="500" show-word-limit
            placeholder="描述您观测到的空气情况：如气味、扬尘、能见度、附近污染源等…" />
        </div>

        <!-- 当前等级参考卡 -->
        <div class="grade-hint" :style="{ borderColor: hintColor }">
          <i class="fa-solid fa-circle-info" :style="{ color: hintColor }"></i>
          <span>{{ hintText }}</span>
        </div>

        <el-button type="primary" class="app-btn-primary app-btn-block" size="large" :loading="submitting"
          @click="handleSubmit">
          <i class="fa-solid fa-paper-plane"></i>提交反馈
        </el-button>
      </template>
    </div>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import { saveAqiFeedback } from '../../api/aqiFeedback'
import { AQI_GRADES } from '../../constants/aqi'

const LEVEL_TIPS = {
  1: { color: '#16a34a', text: '一级（优）：空气质量令人满意，基本无空气污染，各类人群可正常活动。' },
  2: { color: '#65a30d', text: '二级（良）：空气质量可接受，极少数异常敏感人群应减少户外活动。' },
  3: { color: '#d97706', text: '三级（轻度污染）：易感人群症状有轻度加剧，健康人群出现刺激症状。' },
  4: { color: '#dc2626', text: '四级（中度污染）：进一步加剧易感人群症状，可能对健康人群心脏、呼吸系统有影响。' },
  5: { color: '#be123c', text: '五级（重度污染）：心脏病和肺病患者症状显著加剧，健康人群普遍出现症状。' },
  6: { color: '#7f1d1d', text: '六级（严重污染）：健康人群运动耐受力降低，有明显强烈症状，提前出现某些疾病。' }
}

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
      submitting: false
    }
  },
  computed: {
    profile() {
      return this.$store.state.profile
    },
    user() {
      return this.$store.state.user || {}
    },
    hint() {
      return LEVEL_TIPS[this.form.estimatedGrade] || LEVEL_TIPS[1]
    },
    hintColor() {
      return this.hint.color
    },
    hintText() {
      return this.hint.text
    }
  },
  created() {
    // 默认观测时间为当前时间
    const now = new Date()
    const pad = n => String(n).padStart(2, '0')
    this.observedAt = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}`
  },
  methods: {
    levelColor(grade) {
      return (LEVEL_TIPS[grade] || LEVEL_TIPS[1]).color
    },
    async handleSubmit() {
      if (!this.form.information) {
        ElMessage.warning('请填写空气质量描述')
        return
      }
      this.submitting = true
      try {
        const [date, time] = this.observedAt ? this.observedAt.split(' ') : ['', '']
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
        ElMessage.success('提交成功，等待管理员指派网格员检测')
        this.form.information = ''
      } catch (err) {
        ElMessage.error(err.message || '提交失败，请确认后端服务已启动')
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style scoped>
/* 桌面端：表单卡片居中限宽，不占满整屏 */
.app-card {
  max-width: 920px;
  margin: 0 auto;
}

/* 已绑定地址条 */
.app-profile-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--app-soft);
  border: 1px dashed var(--app-ring);
  border-radius: 16px;
  padding: 14px 16px;
  margin-bottom: 20px;
}

.profile-pin {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  background: #fff;
  color: var(--app-strong);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  flex-shrink: 0;
}

.profile-main {
  flex: 1;
  min-width: 0;
}

.profile-region {
  font-size: 14.5px;
  font-weight: 700;
  color: var(--app-strong);
}

.profile-addr {
  margin-top: 3px;
  font-size: 12.5px;
  color: #64748b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-edit {
  border: none;
  background: transparent;
  color: var(--app-strong);
  font-size: 13px;
  cursor: pointer;
  padding: 8px;
  border-radius: 10px;
  flex-shrink: 0;
}

.profile-edit:hover {
  background: rgba(255, 255, 255, 0.8);
}

/* 表单字段 */
.app-field {
  margin-bottom: 18px;
}

.app-field-label {
  display: block;
  font-size: 13.5px;
  font-weight: 600;
  color: #334155;
  margin-bottom: 8px;
}

.grade-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.grade-option-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  flex-shrink: 0;
}

/* 等级提示 */
.grade-hint {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  border: 1.5px dashed;
  border-radius: 14px;
  padding: 12px 14px;
  font-size: 12.5px;
  line-height: 1.7;
  color: #475569;
  background: #fafcfb;
  margin-bottom: 18px;
}

.grade-hint i {
  margin-top: 3px;
  font-size: 14px;
}

/* Element Plus 细节（圆角与输入高度更贴合 App 风格） */
:deep(.el-input__wrapper),
:deep(.el-textarea__inner),
:deep(.el-select__wrapper) {
  border-radius: 14px;
}
</style>
