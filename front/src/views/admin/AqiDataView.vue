<template>
  <div class="nep-page">
    <div class="nep-card">
      <div class="nep-card-header">
        <div>
          <h2 class="nep-card-title">
            <span class="nep-title-icon"><i class="fa-solid fa-flask-vial"></i></span>
            确认 AQI 数据列表
          </h2>
          <p class="nep-card-sub">
            浏览网格员提交的实测AQI数据，确认无误后纳入统计（AQI = MAX(SO2, CO, PM2.5)）
            <el-tag v-if="mock" type="warning" size="small" effect="plain" style="margin-left:8px">演示数据</el-tag>
          </p>
        </div>
        <el-button :icon="Refresh" circle @click="fetchList" />
      </div>

      <!-- 查询条件 -->
      <div class="filter-bar">
        <el-input v-model="filters.keyword" placeholder="地区 / 地址 / 网格员编号" clearable class="f-kw"
          @keyup.enter="applyFilter">
          <template #prefix><i class="fa-solid fa-magnifying-glass"></i></template>
        </el-input>
        <el-select v-model="filters.grade" placeholder="AQI等级" clearable class="f-sm">
          <el-option v-for="g in gradeOptions" :key="g.value" :value="g.value" :label="g.label" />
        </el-select>
        <el-select v-model="filters.state" placeholder="状态" clearable class="f-sm">
          <el-option label="待确认" :value="0" />
          <el-option label="已确认" :value="1" />
          <el-option label="已退回" :value="2" />
        </el-select>
        <el-button type="primary" class="nep-btn-gradient" @click="applyFilter">
          <i class="fa-solid fa-magnifying-glass" style="margin-right:4px"></i>查询
        </el-button>
        <el-button @click="resetFilter">
          <i class="fa-solid fa-rotate-left" style="margin-right:4px"></i>重置
        </el-button>
      </div>

      <div class="nep-table-wrap">
        <el-table v-loading="loading" :data="pagedList" style="width: 100%"
          :header-cell-style="{ background: '#f8faf9' }">
          <el-table-column prop="dataId" label="数据编号" width="86" />
          <el-table-column label="网格区域" min-width="150">
            <template #default="{ row }">
              <span class="region"><i class="fa-solid fa-location-dot region-icon"></i>
                {{ row.provinceName }} · {{ row.cityName }}</span>
            </template>
          </el-table-column>
          <el-table-column label="SO₂" width="80" align="center">
            <template #default="{ row }">
              <span class="p-grade" :style="{ color: pColor(row.so2Grade) }">{{ pShort(row.so2Grade) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="CO" width="80" align="center">
            <template #default="{ row }">
              <span class="p-grade" :style="{ color: pColor(row.coGrade) }">{{ pShort(row.coGrade) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="PM2.5" width="80" align="center">
            <template #default="{ row }">
              <span class="p-grade" :style="{ color: pColor(row.pm25Grade) }">{{ pShort(row.pm25Grade) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="AQI等级" width="125" align="center">
            <template #default="{ row }">
              <GradeTag :grade="row.aqiGrade" />
            </template>
          </el-table-column>
          <el-table-column prop="gridCode" label="检测网格员" width="105">
            <template #default="{ row }">
              <span class="muted">{{ row.gridCode }}</span>
            </template>
          </el-table-column>
          <el-table-column label="提交时间" width="165">
            <template #default="{ row }">
              <span class="muted">{{ row.submitDate }} {{ row.submitTime }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="92" align="center">
            <template #default="{ row }">
              <span class="data-state" :class="'d' + row.state">{{ dataStateText(row.state) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="165" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="showDetail(row)">详情</el-button>
              <template v-if="row.state === 0">
                <el-button link type="success" size="small" :loading="confirming" @click="confirmData(row)">确认</el-button>
                <el-button link type="danger" size="small" @click="handleReject(row)">退回</el-button>
              </template>
            </template>
          </el-table-column>
          <template #empty>
            <div class="nep-empty" style="padding:40px">
              <span class="nep-empty-icon"><i class="fa-regular fa-folder-open"></i></span>
              <span>暂无数据</span>
            </div>
          </template>
        </el-table>

        <div class="pager">
          <el-pagination v-model:current-page="page.current" v-model:page-size="page.size"
            :total="filteredList.length" :page-sizes="[10, 20, 50]" background
            layout="total, sizes, prev, pager, next" />
        </div>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="实测 AQI 数据详情" width="620px">
      <el-descriptions :column="2" border v-if="detail">
        <el-descriptions-item label="数据编号">{{ detail.dataId }}</el-descriptions-item>
        <el-descriptions-item label="对应反馈编号">{{ detail.afId }}</el-descriptions-item>
        <el-descriptions-item label="网格区域" :span="2">
          {{ detail.provinceName }} · {{ detail.cityName }}（{{ detail.address }}）
        </el-descriptions-item>
        <el-descriptions-item label="SO₂ 二氧化硫">
          <span :style="{ color: pColor(detail.so2Grade), fontWeight: 600 }">{{ gradeText(detail.so2Grade) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="CO 一氧化碳">
          <span :style="{ color: pColor(detail.coGrade), fontWeight: 600 }">{{ gradeText(detail.coGrade) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="PM2.5 悬浮颗粒物">
          <span :style="{ color: pColor(detail.pm25Grade), fontWeight: 600 }">{{ gradeText(detail.pm25Grade) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="最终 AQI 等级">
          <GradeTag :grade="detail.aqiGrade" />
        </el-descriptions-item>
        <el-descriptions-item label="检测网格员">{{ detail.gridCode }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ detail.submitDate }} {{ detail.submitTime }}</el-descriptions-item>
        <el-descriptions-item label="当前状态" :span="2">
          <span class="data-state" :class="'d' + detail.state">{{ dataStateText(detail.state) }}</span>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="detail && detail.state === 0" type="primary" class="nep-btn-gradient"
          @click="detailVisible = false; confirmData(detail)">确认纳入统计</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import GradeTag from '../../components/GradeTag.vue'
import { getAqiDataList, rejectAqiData, confirmAqiData } from '../../api/task'
import { AQI_GRADES, gradeText } from '../../constants/aqi'

const P_COLORS = ['', '#16a34a', '#65a30d', '#d97706', '#dc2626', '#be123c', '#7f1d1d']
const P_SHORTS = ['', '优', '良', '轻度', '中度', '重度', '严重']

export default {
  name: 'AqiDataView',
  components: { GradeTag },
  data() {
    return {
      Refresh,
      list: [],
      loading: true,
      mock: false,
      confirming: false,
      filters: { keyword: '', grade: '', state: '' },
      applied: null,
      page: { current: 1, size: 10 },
      detail: null,
      detailVisible: false,
      gradeOptions: AQI_GRADES
    }
  },
  computed: {
    filteredList() {
      const f = this.applied || { keyword: '', grade: '', state: '' }
      return this.list.filter(item => {
        if (f.keyword) {
          const hay = [item.provinceName, item.cityName, item.address, item.gridCode].join('|')
          if (hay.indexOf(f.keyword) === -1) return false
        }
        if (f.grade !== '' && item.aqiGrade !== Number(f.grade)) return false
        if (f.state !== '' && item.state !== Number(f.state)) return false
        return true
      })
    },
    pagedList() {
      const start = (this.page.current - 1) * this.page.size
      return this.filteredList.slice(start, start + this.page.size)
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    gradeText,
    pColor(g) {
      return P_COLORS[g] || '#94a3b8'
    },
    pShort(g) {
      return P_SHORTS[g] || '未知'
    },
    dataStateText(s) {
      return { 0: '待确认', 1: '已确认', 2: '已退回' }[s] || '未知'
    },
    async fetchList() {
      this.loading = true
      try {
        const res = await getAqiDataList()
        this.list = res.list
        this.mock = res.mock
      } finally {
        this.loading = false
      }
    },
    applyFilter() {
      this.applied = { ...this.filters }
      this.page.current = 1
    },
    resetFilter() {
      this.filters = { keyword: '', grade: '', state: '' }
      this.applied = null
      this.page.current = 1
    },
    showDetail(item) {
      this.detail = item
      this.detailVisible = true
    },
    confirmData(item) {
      this.confirming = true
      confirmAqiData(item.dataId)
        .then(res => {
          item.state = 1
          ElMessage.success('数据已确认' + (res.mock ? '（演示数据）' : '') + '，纳入统计范围')
        })
        .catch(err => {
          ElMessage.error((err && err.message) || '确认失败，请重试')
        })
        .finally(() => { this.confirming = false })
    },
    handleReject(item) {
      ElMessageBox.confirm(
        '退回后该任务将重新变为待指派，需要重新指派检测。确定退回吗？',
        '退回确认',
        { confirmButtonText: '退回', cancelButtonText: '取消', type: 'warning' }
      )
        .then(async () => {
          try {
            const res = await rejectAqiData(item.dataId)
            item.state = 2
            ElMessage.success('已退回' + (res.mock ? '（演示数据）' : '') + '，任务重新进入待指派状态')
          } catch (err) {
            ElMessage.error((err && err.message) || '退回失败，请重试')
          }
        })
        .catch(() => {})
    }
  }
}
</script>

<style scoped>
.filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding: 16px 22px 4px;
}

.f-kw {
  width: 280px;
}

.f-sm {
  width: 150px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding: 14px 12px 6px;
}

.region {
  font-size: 13px;
  color: #334155;
  font-weight: 600;
}

.region-icon {
  color: #10b981;
  margin-right: 4px;
  font-size: 12px;
}

.muted {
  color: #94a3b8;
  font-size: 13px;
}

.p-grade {
  font-weight: 700;
  font-size: 13px;
}

.data-state {
  font-size: 12px;
  font-weight: 600;
  border-radius: 999px;
  padding: 3px 10px;
}

.data-state.d0 {
  color: #7c3aed;
  background: #f5f3ff;
}

.data-state.d1 {
  color: #047857;
  background: #ecfdf5;
}

.data-state.d2 {
  color: #b45309;
  background: #fffbeb;
}
</style>
