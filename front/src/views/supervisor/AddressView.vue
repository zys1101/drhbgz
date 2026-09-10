<template>
  <div class="page">
    <transition name="toast">
      <div v-if="toast.show" class="toast" :class="toast.type">
        <span class="toast-msg">{{ toast.message }}</span>
      </div>
    </transition>

    <div class="card">
      <div class="card-header">
        <h2 class="card-title">网格地址绑定</h2>
        <p class="card-sub">选择您所在的网格区域（省、市），并填写日常观测的具体地址</p>
      </div>

      <form class="address-form" @submit.prevent="handleSave">
        <div class="form-row">
          <div class="form-item">
            <label class="form-label">省份 <span class="required">*</span></label>
            <select v-model="form.provinceId" @change="onProvinceChange" class="form-input">
              <option value="" disabled>请选择省份</option>
              <option v-for="p in provinces" :key="p.provinceId" :value="p.provinceId">
                {{ p.provinceName }}
              </option>
            </select>
          </div>

          <div class="form-item">
            <label class="form-label">城市 <span class="required">*</span></label>
            <select v-model="form.cityId" class="form-input" :disabled="cities.length === 0">
              <option value="" disabled>{{ cities.length === 0 ? '请先选择省份' : '请选择城市' }}</option>
              <option v-for="c in cities" :key="c.cityId" :value="c.cityId">
                {{ c.cityName }}
              </option>
            </select>
          </div>
        </div>

        <div class="form-item">
          <label class="form-label">具体地址 <span class="required">*</span></label>
          <input
            type="text"
            v-model.trim="form.address"
            class="form-input"
            :class="{ 'has-error': errors.address }"
            placeholder="请输入您观测空气质量的具体地址（100字以内）"
            maxlength="100"
          >
          <span v-if="errors.address" class="error-msg">{{ errors.address }}</span>
        </div>

        <div class="form-actions">
          <button type="submit" class="btn btn-primary" :disabled="saving">
            {{ saving ? '保存中...' : '保存地址' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script>
import { getProvinces, getCityByPid } from '../../api/aqiFeedback'

export default {
  name: 'AddressView',
  data() {
    return {
      form: {
        provinceId: '',
        cityId: '',
        address: ''
      },
      provinces: [],
      cities: [],
      errors: {},
      saving: false,
      toast: { show: false, type: 'success', message: '' },
      toastTimer: null
    }
  },
  async created() {
    // 回显已绑定地址
    const profile = this.$store.state.profile
    if (profile) {
      this.form.provinceId = profile.provinceId
      this.form.address = profile.address
      if (profile.provinceId) {
        await this.loadCities(profile.provinceId)
        this.form.cityId = profile.cityId
      }
    }
    this.loadProvinces()
  },
  methods: {
    showToast(type, message) {
      this.toast = { show: true, type, message }
      if (this.toastTimer) clearTimeout(this.toastTimer)
      this.toastTimer = setTimeout(() => { this.toast.show = false }, 2500)
    },
    async loadProvinces() {
      try {
        const res = await getProvinces()
        if (res.data.code === 200) {
          this.provinces = res.data.data || []
        }
      } catch (err) {
        // 后端未提供省市接口时使用演示行政区划
        this.provinces = this.demoProvinces()
        this.showToast('warning', '后端省份接口未连接，当前为演示行政区划')
      }
    },
    demoProvinces() {
      return [
        { provinceId: 1, provinceName: '河北省', cities: [ { cityId: 1, cityName: '石家庄市' }, { cityId: 3, cityName: '保定市' }, { cityId: 4, cityName: '廊坊市' }, { cityId: 5, cityName: '沧州市' } ] },
        { provinceId: 2, provinceName: '辽宁省', cities: [ { cityId: 6, cityName: '沈阳市' }, { cityId: 7, cityName: '大连市' } ] },
        { provinceId: 3, provinceName: '吉林省', cities: [ { cityId: 8, cityName: '长春市' } ] },
        { provinceId: 4, provinceName: '黑龙江省', cities: [ { cityId: 9, cityName: '哈尔滨市' } ] },
        { provinceId: 5, provinceName: '山东省', cities: [ { cityId: 10, cityName: '济南市' }, { cityId: 11, cityName: '青岛市' } ] }
      ]
    },
    async loadCities(provinceId) {
      const province = this.provinces.find(p => p.provinceId === provinceId)
      if (province && province.cities) {
        this.cities = province.cities
        return
      }
      try {
        const res = await getCityByPid(provinceId)
        if (res.data.code === 200) {
          this.cities = res.data.data || []
        }
      } catch (err) {
        this.showToast('error', '城市列表加载失败')
      }
    },
    async onProvinceChange() {
      this.form.cityId = ''
      this.cities = []
      if (this.form.provinceId) {
        await this.loadCities(this.form.provinceId)
      }
    },
    async handleSave() {
      const errors = {}
      if (!this.form.provinceId || !this.form.cityId) {
        this.showToast('warning', '请选择完整网格区域（省、市）')
        return
      }
      if (!this.form.address) {
        errors.address = '请填写有效地址'
      }
      this.errors = errors
      if (Object.keys(errors).length) return

      const province = this.provinces.find(p => p.provinceId === this.form.provinceId)
      const city = this.cities.find(c => c.cityId === this.form.cityId)
      this.saving = true
      // TODO: 后端就绪后保存到用户档案接口
      this.$store.dispatch('saveProfile', {
        provinceId: this.form.provinceId,
        provinceName: province ? province.provinceName : '',
        cityId: this.form.cityId,
        cityName: city ? city.cityName : '',
        address: this.form.address
      })
      this.showToast('success', '地址保存成功')
      this.saving = false
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

.address-form {
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

.form-input.has-error {
  border-color: #f56c6c;
}

.error-msg {
  font-size: 12px;
  color: #f56c6c;
  margin-top: 5px;
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
