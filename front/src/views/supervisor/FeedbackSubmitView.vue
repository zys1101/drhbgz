<template>
  <div class="nep-page">
    <div class="nep-card form-card">
      <div class="nep-card-header">
        <div>
          <h2 class="nep-card-title">
            <span class="nep-title-icon"><i class="fa-solid fa-comment-dots"></i></span>
            提交空气质量监督信息
          </h2>
          <p class="nep-card-sub">参照《空气质量指数（AQI）范围及相应类别表》预估等级并描述观测情况</p>
        </div>
      </div>

      <div class="nep-card-body">
        <!-- 未绑定地址引导 -->
        <div v-if="!profile" class="no-profile">
          <i class="fa-solid fa-map-location-dot"></i>
          <p>您还未绑定网格地址，请先完成地址绑定</p>
          <el-button type="primary" class="nep-btn-gradient" @click="$router.push('/sf/address')">
            <i class="fa-solid fa-location-dot" style="margin-right:6px"></i>去绑定地址
          </el-button>
        </div>

        <el-form v-else label-position="top" size="large">
          <!-- 已绑定地址条 -->
          <div class="profile-bar">
            <div class="profile-region">
              <i class="fa-solid fa-location-dot"></i>
              <b>{{ profile.provinceName }} · {{ profile.cityName }}</b>
            </div>
            <span class="profile-addr">{{ profile.address }}</span>
            <el-link type="primary" :underline="false" @click="$router.push('/sf/address')">
              <i class="fa-solid fa-pen"></i> 修改地址
            </el-link>
          </div>

          <div class="row-2">
            <el-form-item label="预估AQI等级">
              <el-select v-model="form.estimatedGrade" style="width: 100%">
                <el-option v-for="g in gradeOptions" :key="g.value" :value="g.value" :label="g.label" />
              </el-select>
            </el-form-item>
            <el-form-item label="观测时间">
              <el-date-picker v-model="observedAt" type="datetime" style="width: 100%"
                format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DD HH:mm" :clearable="false" />
            </el-form-item>
          </div>

          <el-form-item label="空气质量描述">
            <el-input v-model.trim="form.information" type="textarea" :rows="5" maxlength="500" show-word-limit
              placeholder="描述您观测到的空气情况：如气味、扬尘、能见度、附近污染源等…" />
          </el-form-item>

          <!-- 当前等级参考卡 -->
          <div class="grade-hint" :style="{ borderColor: hintColor }">
            <i class="fa-solid fa-circle-info" :style="{ color: hintColor }"></i>
            <span>{{ hintText }}</span>
          </div>

          <div class="form-actions">
            <el-button type="primary" size="large" class="nep-btn-gradient" :loading="submitting" @click="handleSubmit">
              <i class="fa-solid fa-paper-plane" style="margin-right:6px"></i>提交反馈
            </el-button>
          </div>
        </el-form>
      </div>
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
.form-card {
  max-width: 780px;
  margin: 20px auto 0;
}

/* 未绑定地址 */
.no-profile {
  padding: 60px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  color: #94a3b8;
}

.no-profile i {
  font-size: 52px;
  color: #cbd5e1;
}

.no-profile p {
  margin: 0;
  font-size: 14px;
}

/* 已绑定地址条 */
.profile-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  background: linear-gradient(120deg, #ecfdf5, #f0fdfa);
  border: 1px solid #a7f3d0;
  border-radius: 12px;
  padding: 13px 18px;
  margin-bottom: 22px;
}

.profile-region {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #047857;
  font-size: 14px;
}

.profile-region i {
  font-size: 13px;
}

.profile-addr {
  flex: 1;
  min-width: 160px;
  font-size: 13px;
  color: #334155;
}

.row-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}

/* 等级提示 */
.grade-hint {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  border: 1px dashed;
  border-radius: 10px;
  padding: 12px 14px;
  font-size: 13px;
  color: #475569;
  background: #fafcfb;
  margin-bottom: 6px;
}

.grade-hint i {
  margin-top: 2px;
}

.form-actions {
  display: flex;
  justify-content: center;
  margin-top: 14px;
}

@media (max-width: 640px) {
  .row-2 {
    grid-template-columns: 1fr;
  }
}
</style>
