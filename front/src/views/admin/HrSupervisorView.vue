<template>
  <div class="nep-page">
    <div class="nep-card">
      <div class="nep-card-header">
        <div>
          <h2 class="nep-card-title">
            <span class="nep-title-icon"><i class="fa-solid fa-users"></i></span>
            公众监督员管理
          </h2>
          <p class="nep-card-sub">查看公众监督员档案信息，维护其基本信息与绑定网格地区</p>
        </div>
        <div class="header-actions">
          <el-input v-model="keyword" placeholder="搜索手机号 / 姓名 / 地区" clearable style="width: 220px"
            @keyup.enter="applyFilter" @clear="applyFilter">
            <template #prefix><i class="fa-solid fa-magnifying-glass"></i></template>
          </el-input>
          <el-button type="primary" class="nep-btn-gradient" @click="applyFilter">
            <i class="fa-solid fa-magnifying-glass" style="margin-right:4px"></i>查询
          </el-button>
          <el-button :icon="Refresh" circle @click="fetchList" />
        </div>
      </div>

      <div class="nep-table-wrap">
        <el-table v-loading="loading" :data="pagedList" style="width: 100%"
          :header-cell-style="{ background: '#f8faf9' }">
          <el-table-column prop="telId" label="手机号" width="130" />
          <el-table-column prop="realName" label="姓名" width="100" />
          <el-table-column prop="age" label="年龄" width="70" align="center" />
          <el-table-column prop="gender" label="性别" width="70" align="center" />
          <el-table-column label="绑定地区" min-width="170">
            <template #default="{ row }">
              <span class="region-text">{{ row.provinceName || '未绑定' }}<template v-if="row.cityName"> · {{ row.cityName }}</template></span>
            </template>
          </el-table-column>
          <el-table-column prop="address" label="观测地址" min-width="180" show-overflow-tooltip />
          <el-table-column label="注册时间" width="165">
            <template #default="{ row }">{{ row.registerDate }} {{ row.registerTime }}</template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination v-if="filtered.length > pageSize" class="nep-pagination" background layout="prev, pager, next"
          :total="filtered.length" :page-size="pageSize" v-model:current-page="page" />
      </div>
    </div>

    <!-- 编辑监督员 -->
    <el-dialog v-model="editVisible" title="编辑监督员信息" width="480px">
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="手机号">
          <el-input :model-value="editForm.telId" disabled />
        </el-form-item>
        <el-form-item label="姓名" required>
          <el-input v-model="editForm.realName" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="年龄" required>
          <el-input-number v-model="editForm.age" :min="1" :max="120" style="width: 100%" />
        </el-form-item>
        <el-form-item label="性别" required>
          <el-radio-group v-model="editForm.gender">
            <el-radio value="男">男</el-radio>
            <el-radio value="女">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="绑定省份">
          <el-select v-model="editForm.provinceId" placeholder="选择省份" style="width: 100%" clearable
            @change="onProvinceChange">
            <el-option v-for="p in provinces" :key="p.provinceId" :label="p.provinceName" :value="p.provinceId" />
          </el-select>
        </el-form-item>
        <el-form-item label="绑定城市">
          <el-select v-model="editForm.cityId" placeholder="选择城市" style="width: 100%" clearable
            :disabled="!editForm.provinceId">
            <el-option v-for="c in cities" :key="c.cityId" :label="c.cityName" :value="c.cityId" />
          </el-select>
        </el-form-item>
        <el-form-item label="观测地址">
          <el-input v-model="editForm.address" type="textarea" :rows="2" maxlength="100" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" class="nep-btn-gradient" :loading="saving" @click="saveSupervisor">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getProvinces, getCityByPid } from '../../api/aqiFeedback'
import { getSupervisorList, updateSupervisor } from '../../api/hr'

export default {
  name: 'HrSupervisorView',
  data() {
    return {
      loading: false,
      allList: [],
      filtered: [],
      pagedList: [],
      keyword: '',
      page: 1,
      pageSize: 10,
      provinces: [],
      cities: [],
      editVisible: false,
      saving: false,
      editForm: {}
    }
  },
  created() {
    this.fetchList()
    getProvinces().then(res => {
      if (res.data.code === 200) this.provinces = res.data.data
    }).catch(() => {})
  },
  watch: {
    page() {
      this.updatePaged()
    }
  },
  methods: {
    updatePaged() {
      const start = (this.page - 1) * this.pageSize
      this.pagedList = this.filtered.slice(start, start + this.pageSize)
    },
    async fetchList() {
      this.loading = true
      try {
        const res = await getSupervisorList()
        if (res.data.code === 200) {
          this.allList = res.data.data
          this.applyFilter()
        } else {
          ElMessage.error(res.data.message || '获取监督员列表失败')
        }
      } catch (err) {
        console.error(err)
        ElMessage.error('网络异常，请确认后端服务已启动')
      } finally {
        this.loading = false
      }
    },
    applyFilter() {
      const kw = this.keyword.trim()
      this.filtered = kw
        ? this.allList.filter(s =>
            (s.telId || '').includes(kw) || (s.realName || '').includes(kw) ||
            (s.provinceName || '').includes(kw) || (s.cityName || '').includes(kw))
        : this.allList
      this.page = 1
      this.updatePaged()
    },
    openEdit(row) {
      this.editForm = {
        telId: row.telId,
        realName: row.realName,
        age: row.age,
        gender: row.gender,
        provinceId: row.provinceId,
        cityId: row.cityId,
        address: row.address
      }
      if (row.provinceId) this.onProvinceChange(row.provinceId)
      this.editVisible = true
    },
    async onProvinceChange(provinceId) {
      this.cities = []
      this.editForm.cityId = null
      if (!provinceId) return
      try {
        const res = await getCityByPid(provinceId)
        if (res.data.code === 200) this.cities = res.data.data
      } catch (err) {
        console.error(err)
      }
    },
    async saveSupervisor() {
      const f = this.editForm
      if (!f.realName) {
        ElMessage.warning('请填写姓名')
        return
      }
      this.saving = true
      try {
        const res = await updateSupervisor({
          telId: f.telId,
          realName: f.realName,
          age: f.age,
          gender: f.gender,
          provinceId: f.provinceId,
          cityId: f.cityId,
          address: f.address
        })
        if (res.data.code === 200) {
          ElMessage.success(res.data.message || '更新成功')
          this.editVisible = false
          this.fetchList()
        } else {
          ElMessage.error(res.data.message || '更新失败')
        }
      } catch (err) {
        console.error(err)
        ElMessage.error('网络异常，请确认后端服务已启动')
      } finally {
        this.saving = false
      }
    }
  }
}
</script>

<style scoped>
.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.region-text {
  color: #334155;
  font-size: 13px;
}
</style>
