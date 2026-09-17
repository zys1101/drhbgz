<template>
  <div class="nep-page">
    <div class="nep-card form-card" v-loading="loadingData" element-loading-text="数据加载中...">
      <div class="nep-card-header">
        <div>
          <h2 class="nep-card-title">
            <span class="nep-title-icon"><i class="fa-solid fa-pen-to-square"></i></span>
            {{ form.aqiId ? '修改空气质量指数级别' : '新增空气质量指数级别' }}
          </h2>
          <p class="nep-card-sub">维护《空气质量指数（AQI）范围及相应类别表》与污染物浓度限值</p>
        </div>
        <el-button text @click="goBack"><i class="fa-solid fa-arrow-left"></i> 返回列表</el-button>
      </div>

      <div class="nep-card-body">
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large" class="aqi-form">
          <div class="row-2">
            <el-form-item label="级别汉字表述" prop="chineseExplain">
              <el-input v-model="form.chineseExplain" placeholder="如：一级" maxlength="10" />
            </el-form-item>
            <el-form-item label="级别描述" prop="aqiExplain">
              <el-input v-model="form.aqiExplain" placeholder="如：优" maxlength="10" />
            </el-form-item>
          </div>

          <el-form-item label="级别颜色" prop="color">
            <div class="color-row">
              <el-color-picker v-model="form.color" show-alpha :predefine="predefineColors" />
              <el-input v-model="form.color" placeholder="#RRGGBB" maxlength="9" class="color-text" />
              <span class="level-preview" :style="{ background: form.color, color: getContrastColor(form.color) }">
                {{ form.chineseExplain || '一级' }} · {{ form.aqiExplain || '优' }}
              </span>
            </div>
          </el-form-item>

          <el-divider content-position="left">
            <i class="fa-solid fa-industry" style="margin-right:6px;color:#10b981"></i>污染物浓度限值
          </el-divider>

          <div class="row-3">
            <el-form-item label="SO₂ 最小值" prop="so2Min">
              <el-input-number v-model="form.so2Min" :min="0" :max="10000" controls-position="right" style="width:100%" />
            </el-form-item>
            <el-form-item label="SO₂ 最大值" prop="so2Max">
              <el-input-number v-model="form.so2Max" :min="0" :max="10000" controls-position="right" style="width:100%" />
            </el-form-item>
            <div class="unit-tip">μg/m³</div>
          </div>
          <div class="row-3">
            <el-form-item label="CO 最小值" prop="coMin">
              <el-input-number v-model="form.coMin" :min="0" :max="10000" controls-position="right" style="width:100%" />
            </el-form-item>
            <el-form-item label="CO 最大值" prop="coMax">
              <el-input-number v-model="form.coMax" :min="0" :max="10000" controls-position="right" style="width:100%" />
            </el-form-item>
            <div class="unit-tip">mg/m³</div>
          </div>
          <div class="row-3">
            <el-form-item label="PM2.5 最小值" prop="spmMin">
              <el-input-number v-model="form.spmMin" :min="0" :max="10000" controls-position="right" style="width:100%" />
            </el-form-item>
            <el-form-item label="PM2.5 最大值" prop="spmMax">
              <el-input-number v-model="form.spmMax" :min="0" :max="10000" controls-position="right" style="width:100%" />
            </el-form-item>
            <div class="unit-tip">μg/m³</div>
          </div>

          <el-form-item label="对健康影响情况" prop="healthImpact">
            <el-input v-model="form.healthImpact" type="textarea" :rows="3" maxlength="200" show-word-limit
              placeholder="请描述该级别对健康的影响..." />
          </el-form-item>
          <el-form-item label="建议采取的措施" prop="takeSteps">
            <el-input v-model="form.takeSteps" type="textarea" :rows="3" maxlength="200" show-word-limit
              placeholder="请描述该级别建议采取的防护措施..." />
          </el-form-item>

          <div class="form-actions">
            <el-button size="large" @click="resetForm">
              <i class="fa-solid fa-rotate-left" style="margin-right:6px"></i>重 置
            </el-button>
            <el-button type="primary" size="large" class="nep-btn-gradient" :loading="submitting" @click="handleSubmit">
              <i class="fa-solid fa-check" style="margin-right:6px"></i>
              {{ submitting ? '提交中...' : (form.aqiId ? '保存修改' : '提 交') }}
            </el-button>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import { saveAqi, getAqiById, updateAqi } from '../api/aqi.js'

export default {
  name: 'AqiForm',
  data() {
    return {
      form: {
        aqiId: null,
        chineseExplain: '',
        aqiExplain: '',
        color: '#10b981',
        healthImpact: '',
        takeSteps: '',
        so2Min: 0,
        so2Max: 0,
        coMin: 0,
        coMax: 0,
        spmMin: 0,
        spmMax: 0
      },
      rules: {
        chineseExplain: [{ required: true, message: '请输入级别汉字表述', trigger: 'blur' }],
        aqiExplain: [{ required: true, message: '请输入级别描述', trigger: 'blur' }],
        color: [{ required: true, message: '请选择级别颜色', trigger: 'change' }],
        healthImpact: [{ required: true, message: '请输入对健康影响情况', trigger: 'blur' }],
        takeSteps: [{ required: true, message: '请输入建议采取的措施', trigger: 'blur' }]
      },
      predefineColors: ['#00e400', '#ffff00', '#ff7e00', '#ff0000', '#99004c', '#7f0023'],
      loadingData: false,
      submitting: false
    }
  },
  async mounted() {
    const aqiId = this.$route.query.aqiId
    if (aqiId) {
      this.loadingData = true
      try {
        const res = await getAqiById(aqiId)
        const detail = res.data.data
        Object.assign(this.form, {
          aqiId: detail.aqiId,
          chineseExplain: detail.chineseExplain,
          aqiExplain: detail.aqiExplain,
          color: detail.color || '#10b981',
          healthImpact: detail.healthImpact,
          takeSteps: detail.takeSteps,
          so2Min: detail.so2Min,
          so2Max: detail.so2Max,
          coMin: detail.coMin,
          coMax: detail.coMax,
          spmMin: detail.spmMin,
          spmMax: detail.spmMax
        })
      } catch (err) {
        ElMessage.error('数据加载失败，请刷新重试')
      } finally {
        this.loadingData = false
      }
    }
  },
  methods: {
    goBack() {
      this.$router.push('/aqi')
    },
    getContrastColor(hex) {
      if (!hex || hex.length < 7 || hex[0] !== '#') return '#fff'
      const r = parseInt(hex.slice(1, 3), 16)
      const g = parseInt(hex.slice(3, 5), 16)
      const b = parseInt(hex.slice(5, 7), 16)
      const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255
      return luminance > 0.6 ? '#333' : '#fff'
    },
    handleSubmit() {
      this.$refs.formRef.validate(async valid => {
        if (!valid) {
          ElMessage.warning('请完整填写表单')
          return
        }
        this.submitting = true
        try {
          const res = this.form.aqiId
            ? await updateAqi({ ...this.form })
            : await saveAqi({ ...this.form })
          if (res.data.code === 200) {
            ElMessage.success(this.form.aqiId ? '修改成功' : '保存成功')
            this.$router.push('/aqi')
          } else {
            ElMessage.error(res.data.message || '提交失败')
          }
        } catch (err) {
          ElMessage.error('提交失败，请重试')
        } finally {
          this.submitting = false
        }
      })
    },
    resetForm() {
      this.$refs.formRef.resetFields()
      this.form = {
        aqiId: this.form.aqiId,
        chineseExplain: '',
        aqiExplain: '',
        color: '#10b981',
        healthImpact: '',
        takeSteps: '',
        so2Min: 0,
        so2Max: 0,
        coMin: 0,
        coMax: 0,
        spmMin: 0,
        spmMax: 0
      }
    }
  }
}
</script>

<style scoped>
.form-card {
  max-width: 880px;
  margin: 20px auto 0;
}

.aqi-form {
  max-width: 780px;
}

.row-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}

.row-3 {
  display: grid;
  grid-template-columns: 1fr 1fr 110px;
  gap: 18px;
  align-items: end;
}

.unit-tip {
  color: #94a3b8;
  font-size: 13px;
  padding-bottom: 14px;
  text-align: center;
}

.color-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.color-text {
  width: 140px;
}

.level-preview {
  display: inline-block;
  border-radius: 999px;
  padding: 5px 15px;
  font-size: 12.5px;
  font-weight: 700;
  box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.05);
}

.form-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 10px;
}

@media (max-width: 760px) {
  .row-2,
  .row-3 {
    grid-template-columns: 1fr;
  }
}
</style>
