<template>
  <div class="nep-page">
    <div class="nep-card">
      <div class="nep-card-header">
        <div>
          <h2 class="nep-card-title">
            <span class="nep-title-icon"><i class="fa-solid fa-table-cells"></i></span>
            AQI 级别管理
          </h2>
          <p class="nep-card-sub">空气质量指数（AQI）范围及相应类别表 · 污染物项目浓度限值</p>
        </div>
        <el-button type="primary" class="nep-btn-gradient" @click="$router.push('/aqiForm')">
          <i class="fa-solid fa-plus" style="margin-right:6px"></i>新增级别
        </el-button>
      </div>

      <div class="nep-table-wrap">
        <el-table v-loading="loading" :data="list" style="width: 100%"
          :header-cell-style="{ background: '#f8faf9' }">
          <el-table-column label="级别" width="150">
            <template #default="{ row }">
              <span class="level-chip" :style="{ background: row.color, color: getContrastColor(row.color) }">
                {{ row.chineseExplain }} · {{ row.aqiExplain }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="aqiRange" label="AQI指数范围" width="110" align="center" />
          <el-table-column label="SO₂ (μg/m³)" width="120" align="center">
            <template #default="{ row }"><span class="muted">{{ row.so2Min }} ~ {{ row.so2Max }}</span></template>
          </el-table-column>
          <el-table-column label="CO (mg/m³)" width="120" align="center">
            <template #default="{ row }"><span class="muted">{{ row.coMin }} ~ {{ row.coMax }}</span></template>
          </el-table-column>
          <el-table-column label="PM2.5 (μg/m³)" width="130" align="center">
            <template #default="{ row }"><span class="muted">{{ row.spmMin }} ~ {{ row.spmMax }}</span></template>
          </el-table-column>
          <el-table-column prop="healthImpact" label="对健康影响" min-width="220" show-overflow-tooltip />
          <el-table-column prop="takeSteps" label="建议措施" min-width="220" show-overflow-tooltip />
          <el-table-column label="操作" width="130" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small"
                @click="$router.push({ path: '/aqiForm', query: { aqiId: row.aqiId } })">编辑</el-button>
              <el-button link type="danger" size="small" @click="handleDelete(row.aqiId)">删除</el-button>
            </template>
          </el-table-column>
          <template #empty>
            <div class="nep-empty" style="padding:40px">
              <span class="nep-empty-icon"><i class="fa-regular fa-folder-open"></i></span>
              <span>暂无数据</span>
            </div>
          </template>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAqiList, deleteAqi } from '../api/aqi.js'

export default {
  name: 'AqiList',
  data() {
    return {
      list: [],
      loading: true
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    async fetchList() {
      this.loading = true
      try {
        const res = await getAqiList()
        if (res.data.code === 200) {
          this.list = res.data.data || []
          if (res.mock) {
            ElMessage.warning('后端级别接口未连接，当前为标准AQI级别演示数据')
          }
        }
      } catch (err) {
        this.list = []
        ElMessage.error('获取级别列表失败')
      } finally {
        this.loading = false
      }
    },
    getContrastColor(hex) {
      if (!hex || hex.length < 7) return '#fff'
      const r = parseInt(hex.slice(1, 3), 16)
      const g = parseInt(hex.slice(3, 5), 16)
      const b = parseInt(hex.slice(5, 7), 16)
      const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255
      return luminance > 0.6 ? '#333' : '#fff'
    },
    handleDelete(aqiId) {
      ElMessageBox.confirm('确定要删除该级别吗？删除后不可恢复。', '删除确认', {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      })
        .then(async () => {
          try {
            const res = await deleteAqi(aqiId)
            if (res.data.code === 200) {
              ElMessage.success('删除成功')
              this.fetchList()
            } else {
              ElMessage.error(res.data.message || '删除失败')
            }
          } catch (err) {
            ElMessage.error('网络异常，请稍后重试')
          }
        })
        .catch(() => {})
    }
  }
}
</script>

<style scoped>
.level-chip {
  display: inline-block;
  border-radius: 999px;
  padding: 4px 13px;
  font-size: 12.5px;
  font-weight: 700;
  white-space: nowrap;
  box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.05);
}

.muted {
  color: #94a3b8;
  font-size: 12.5px;
  font-variant-numeric: tabular-nums;
}
</style>
