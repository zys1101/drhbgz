<template>
  <div class="app-page">
    <div class="app-card">
      <div class="app-card-head">
        <div>
          <h2 class="app-card-title">
            <span class="app-ico"><i class="fa-solid fa-location-dot"></i></span>
            网格地址绑定
          </h2>
          <p class="app-card-sub">选择您所在的网格区域（省、市），并填写日常观测的具体地址</p>
        </div>
      </div>

      <el-form label-position="top" size="large" class="address-form">
        <div class="row-2">
          <el-form-item label="省份">
            <el-select v-model="form.provinceId" placeholder="请选择省份" filterable style="width: 100%"
              @change="onProvinceChange">
              <el-option v-for="p in provinces" :key="p.provinceId" :value="p.provinceId" :label="p.provinceName" />
            </el-select>
          </el-form-item>
          <el-form-item label="城市">
            <el-select v-model="form.cityId" placeholder="请先选择省份" filterable style="width: 100%"
              :loading="cityLoading">
              <el-option v-for="c in cities" :key="c.cityId" :value="c.cityId" :label="c.cityName" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="具体地址">
          <el-input v-model.trim="form.address" maxlength="100" show-word-limit clearable
            placeholder="请输入您观测空气质量的具体地址（100字以内）">
            <template #prefix><i class="fa-solid fa-map-pin"></i></template>
          </el-input>
        </el-form-item>
        <el-button type="primary" size="large" class="app-btn-primary app-btn-block" :loading="saving" @click="handleSave">
          <i class="fa-solid fa-floppy-disk"></i>保存地址
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import { getProvinces, getCityByPid } from '../../api/aqiFeedback'
import { getProfile, saveProfile } from '../../api/auth'

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
      cityLoading: false,
      saving: false
    }
  },
  computed: {
    myAccount() {
      return this.$store.state.user ? this.$store.state.user.account : ''
    }
  },
  async created() {
    // 回显已绑定地址（优先本地，本地为空则从后端档案拉取）
    const profile = this.$store.state.profile
    if (profile) {
      this.form.provinceId = profile.provinceId
      this.form.address = profile.address
      if (profile.provinceId) {
        await this.loadCities(profile.provinceId)
        this.form.cityId = profile.cityId
      }
    } else if (this.myAccount) {
      try {
        const res = await getProfile(this.myAccount)
        const sup = res.data
        if (sup && sup.provinceId) {
          await this.loadCities(sup.provinceId)
          this.form.provinceId = sup.provinceId
          this.form.cityId = sup.cityId
          this.form.address = sup.address || ''
          this.persistProfile(sup)
        }
      } catch (e) { /* 忽略，保持空表单 */ }
    }
    this.loadProvinces()
  },
  methods: {
    async loadProvinces() {
      try {
        const res = await getProvinces()
        if (res.data.code === 200) {
          this.provinces = res.data.data || []
        }
      } catch (err) {
        // 后端未提供省市接口时使用演示行政区划
        this.provinces = this.demoProvinces()
        ElMessage.warning('后端省份接口未连接，当前为演示行政区划')
      }
    },
    demoProvinces() {
      return [
        { provinceId: 1, provinceName: '河北省', cities: [{ cityId: 1, cityName: '石家庄市' }, { cityId: 3, cityName: '保定市' }, { cityId: 4, cityName: '廊坊市' }, { cityId: 5, cityName: '沧州市' }] },
        { provinceId: 2, provinceName: '辽宁省', cities: [{ cityId: 6, cityName: '沈阳市' }, { cityId: 7, cityName: '大连市' }] },
        { provinceId: 3, provinceName: '吉林省', cities: [{ cityId: 8, cityName: '长春市' }] },
        { provinceId: 4, provinceName: '黑龙江省', cities: [{ cityId: 9, cityName: '哈尔滨市' }] },
        { provinceId: 5, provinceName: '山东省', cities: [{ cityId: 10, cityName: '济南市' }, { cityId: 11, cityName: '青岛市' }] }
      ]
    },
    async loadCities(provinceId) {
      const province = this.provinces.find(p => p.provinceId === provinceId)
      if (province && province.cities) {
        this.cities = province.cities
        return
      }
      this.cityLoading = true
      try {
        const res = await getCityByPid(provinceId)
        if (res.data.code === 200) {
          this.cities = res.data.data || []
        }
      } catch (err) {
        ElMessage.error('城市列表加载失败')
      } finally {
        this.cityLoading = false
      }
    },
    async onProvinceChange() {
      this.form.cityId = ''
      this.cities = []
      if (this.form.provinceId) {
        await this.loadCities(this.form.provinceId)
      }
    },
    persistProfile(sup, province, city) {
      this.$store.dispatch('saveProfile', {
        provinceId: sup.provinceId,
        provinceName: (province && province.provinceName) || sup.provinceName || '',
        cityId: sup.cityId,
        cityName: (city && city.cityName) || sup.cityName || '',
        address: sup.address
      })
    },
    async handleSave() {
      if (!this.form.provinceId || !this.form.cityId) {
        ElMessage.warning('请选择完整网格区域（省、市）')
        return
      }
      if (!this.form.address) {
        ElMessage.warning('请填写有效地址')
        return
      }
      const province = this.provinces.find(p => p.provinceId === this.form.provinceId)
      const city = this.cities.find(c => c.cityId === this.form.cityId)
      this.saving = true
      try {
        // 保存到监督员档案接口，后端未连接时降级为本地保存
        const res = await saveProfile({
          account: this.myAccount,
          provinceId: this.form.provinceId,
          cityId: this.form.cityId,
          address: this.form.address
        })
        const sup = res.data || {}
        this.persistProfile(sup, province, city)
        ElMessage.success('地址保存成功' + (res.mock ? '（本地演示）' : ''))
      } catch (err) {
        ElMessage.error(err.message || '地址保存失败')
      } finally {
        this.saving = false
      }
    }
  }
}
</script>

<style scoped>
/* 桌面端：表单卡片居中限宽 */
.app-card {
  max-width: 760px;
  margin: 0 auto;
}

.address-form {
  max-width: 640px;
  margin: 0 auto;
}

.row-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}

@media (max-width: 640px) {
  .row-2 {
    grid-template-columns: 1fr;
  }
}
</style>
