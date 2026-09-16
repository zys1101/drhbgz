<template>
  <div class="nep-page">
    <div class="nep-card">
      <div class="nep-card-header">
        <div>
          <h2 class="nep-card-title">
            <span class="nep-title-icon"><i class="fa-solid fa-inbox"></i></span>
            公众监督数据列表
          </h2>
          <p class="nep-card-sub">浏览公众监督员反馈的数据，按条件查询并指派网格员实地检测</p>
        </div>
        <el-button :icon="Refresh" circle @click="fetchList" />
      </div>

      <!-- 查询条件 -->
      <div class="filter-bar">
        <el-input v-model="filters.keyword" placeholder="地区 / 地址 / 手机号 / 描述关键字" clearable class="f-kw"
          @keyup.enter="applyFilter">
          <template #prefix><i class="fa-solid fa-magnifying-glass"></i></template>
        </el-input>
        <el-select v-model="filters.grade" placeholder="预估等级" clearable class="f-sm">
          <el-option v-for="g in gradeOptions" :key="g.value" :value="g.value" :label="g.label" />
        </el-select>
        <el-select v-model="filters.state" placeholder="状态" clearable class="f-sm">
          <el-option v-for="s in stateOptions" :key="s.value" :value="s.value" :label="s.label" />
        </el-select>
        <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="反馈开始"
          end-placeholder="反馈结束" value-format="YYYY-MM-DD" class="f-date" />
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
          <el-table-column prop="afId" label="编号" width="70" />
          <el-table-column label="网格区域" min-width="150">
            <template #default="{ row }">
              <span class="region"><i class="fa-solid fa-location-dot region-icon"></i>
                {{ row.provinceName }} · {{ row.cityName }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="address" label="具体地址" min-width="150" show-overflow-tooltip />
          <el-table-column label="反馈人" width="120">
            <template #default="{ row }">
              <span class="muted">{{ maskTel(row.telId) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="预估AQI等级" width="125" align="center">
            <template #default="{ row }">
              <GradeTag :grade="row.estimatedGrade" />
            </template>
          </el-table-column>
          <el-table-column label="反馈时间" width="165">
            <template #default="{ row }">
              <span class="muted">{{ row.afDate }} {{ row.afTime }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="95" align="center">
            <template #default="{ row }">
              <StateTag :state="row.state" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="showDetail(row)">详情</el-button>
              <el-button v-if="row.state === 0 || row.state === 2" link type="warning" size="small"
                @click="openAssign(row)">指派</el-button>
              <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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
    <el-dialog v-model="detailVisible" title="反馈数据详情" width="600px">
      <el-descriptions :column="2" border v-if="detail">
        <el-descriptions-item label="反馈编号">{{ detail.afId }}</el-descriptions-item>
        <el-descriptions-item label="反馈人手机号">{{ detail.telId }}</el-descriptions-item>
        <el-descriptions-item label="网格区域" :span="2">
          {{ detail.provinceName }} · {{ detail.cityName }}
        </el-descriptions-item>
        <el-descriptions-item label="具体地址" :span="2">{{ detail.address }}</el-descriptions-item>
        <el-descriptions-item label="预估AQI等级">
          <GradeTag :grade="detail.estimatedGrade" />
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <StateTag :state="detail.state" />
        </el-descriptions-item>
        <el-descriptions-item label="反馈时间">{{ detail.afDate }} {{ detail.afTime }}</el-descriptions-item>
        <el-descriptions-item label="指派网格员">
          {{ detail.gridName ? detail.gridName + '（' + detail.gridCode + '）' : '未指派' }}
        </el-descriptions-item>
        <el-descriptions-item label="空气质量描述" :span="2">
          <span class="pre-wrap">{{ detail.information }}</span>
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.remarks" label="备注" :span="2">{{ detail.remarks }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="detail && (detail.state === 0 || detail.state === 2)" type="primary"
          class="nep-btn-gradient" @click="detailVisible = false; openAssign(detail)">指派网格员</el-button>
      </template>
    </el-dialog>

    <!-- 指派弹窗 -->
    <el-dialog v-model="assignVisible" title="指派网格员" width="640px"
      :close-on-click-modal="false">
      <template v-if="assignItem">
        <div class="assign-target">
          <i class="fa-solid fa-location-dot"></i>
          <b>{{ assignItem.provinceName }} · {{ assignItem.cityName }}</b>
          <span class="assign-addr">{{ assignItem.address }}</span>
        </div>

        <!-- 指派规则：只允许本地指派（省+市一致）；名单未就绪时不提前给出结论 -->
        <el-alert v-if="workersLoading" title="正在加载网格员名单…" type="info" :closable="false"
          style="margin-bottom: 14px" />
        <el-alert v-else-if="localWorkers.length" :title="assignModeText" type="success" :closable="false"
          style="margin-bottom: 14px" />
        <el-alert v-else title="该网格区域没有可工作的本地网格员，不允许异地指派，请发起增员请求" type="warning"
          :closable="false" style="margin-bottom: 14px" />

        <!-- 仅列出本地（本网格区域）可工作的网格员，异地网格员不作为可选项 -->
        <div v-if="!workersLoading && localWorkers.length" class="worker-list">
          <label v-for="w in localWorkers" :key="w.gridCode" class="worker-item"
            :class="{ selected: chosenWorker === w.gridCode, local: w.region === assignRegion }">
            <input v-model="chosenWorker" type="radio" :value="w.gridCode" class="worker-radio">
            <div class="worker-avatar">{{ w.realName ? w.realName.charAt(0) : '?' }}</div>
            <div class="worker-meta">
              <span class="worker-name">{{ w.realName }} <code>{{ w.gridCode }}</code></span>
              <span class="worker-region">{{ w.region }}</span>
            </div>
            <span class="local-tag">本地</span>
          </label>
        </div>
      </template>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <!-- 无本地可工作网格员时，确认指派保持禁用 -->
        <el-button type="primary" class="nep-btn-gradient" :disabled="!localWorkers.length || !chosenWorker"
          :loading="assigning2" @click="confirmAssign">
          <i class="fa-solid fa-paper-plane" style="margin-right:6px"></i>确认指派
        </el-button>
        <!-- 本地无人可派：改为主推“申请增员”，交由管理员/决策者跟进 -->
        <el-button v-if="!workersLoading && !localWorkers.length" type="primary" class="nep-btn-gradient"
          :loading="applyingDemand" @click="applyDemand">
          <i class="fa-solid fa-user-plus" style="margin-right:6px"></i>申请增员
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import GradeTag from '../../components/GradeTag.vue'
import StateTag from '../../components/StateTag.vue'
import { getAqiFeedbackList, deleteByAfid } from '../../api/aqiFeedback'
import { assignTask, getGridWorkers } from '../../api/task'
import { applyGridDemand } from '../../api/gridDemand'
import { AQI_GRADES, FEEDBACK_STATES } from '../../constants/aqi'

export default {
  name: 'FeedbackManageView',
  components: { GradeTag, StateTag },
  data() {
    return {
      Refresh,
      list: [],
      loading: true,
      filters: {
        keyword: '',
        grade: '',
        state: '',
        dateFrom: '',
        dateTo: ''
      },
      applied: null, // 点击“查询”后生效的条件
      dateRange: null,
      page: { current: 1, size: 10 },
      detail: null,
      detailVisible: false,
      assignVisible: false,
      assigning2: false,
      applyingDemand: false, // 增员请求提交中
      assignItem: null,
      workers: [],
      workersLoading: false, // 网格员名单加载中（避免误报“本地无人”）
      chosenWorker: '',
      gradeOptions: AQI_GRADES,
      stateOptions: FEEDBACK_STATES
    }
  },
  computed: {
    assignRegion() {
      return this.assignItem
        ? (this.assignItem.provinceName || '') + '-' + (this.assignItem.cityName || '')
        : ''
    },
    localWorkers() {
      return this.workers.filter(w => w.region === this.assignRegion && w.working)
    },
    assignModeText() {
      // 仅本地指派：可选项只有本网格区域可工作的网格员
      return '本地指派：当前网格区域有 ' + this.localWorkers.length + ' 名可工作的网格员，请选择一名指派'
    },
    filteredList() {
      const f = this.applied || { keyword: '', grade: '', state: '', dateFrom: '', dateTo: '' }
      return this.list.filter(item => {
        if (f.keyword) {
          const hay = [item.provinceName, item.cityName, item.address, item.telId, item.information]
            .join('|')
          if (hay.indexOf(f.keyword) === -1) return false
        }
        if (f.grade !== '' && item.estimatedGrade !== Number(f.grade)) return false
        if (f.state !== '' && item.state !== Number(f.state)) return false
        if (f.dateFrom && item.afDate < f.dateFrom) return false
        if (f.dateTo && item.afDate > f.dateTo) return false
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
    this.loadWorkers()
  },
  methods: {
    maskTel(tel) {
      if (!tel || tel.length < 7) return tel
      return tel.slice(0, 3) + '****' + tel.slice(-4)
    },
    async fetchList() {
      this.loading = true
      try {
        const res = await getAqiFeedbackList()
        if (res.data.code === 200) {
          this.list = res.data.data || []
        } else {
          ElMessage.error('获取列表失败：' + (res.data.message || ''))
        }
      } catch (err) {
        console.error(err)
        ElMessage.error('网络异常，请确认后端服务已启动')
      } finally {
        this.loading = false
      }
    },
    async loadWorkers() {
      this.workersLoading = true
      try {
        const res = await getGridWorkers()
        this.workers = res.list
      } catch (err) {
        console.error(err)
        ElMessage.error('获取网格员名单失败，请稍后重试')
      } finally {
        this.workersLoading = false
      }
    },
    applyFilter() {
      this.applied = {
        ...this.filters,
        dateFrom: this.dateRange && this.dateRange[0] ? this.dateRange[0] : '',
        dateTo: this.dateRange && this.dateRange[1] ? this.dateRange[1] : ''
      }
      this.page.current = 1
    },
    resetFilter() {
      this.filters = { keyword: '', grade: '', state: '', dateFrom: '', dateTo: '' }
      this.dateRange = null
      this.applied = null
      this.page.current = 1
    },
    showDetail(item) {
      this.detail = item
      this.detailVisible = true
    },
    async openAssign(item) {
      this.assignItem = item
      this.chosenWorker = ''
      this.assignVisible = true
      if (!this.workers.length) {
        await this.loadWorkers()
      }
    },
    async confirmAssign() {
      if (!this.chosenWorker) return
      this.assigning2 = true
      try {
        const res = await assignTask(this.assignItem.afId, this.chosenWorker, this.assignItem)
        // 可选项只来自本地网格员，故此处必为本地指派
        ElMessage.success('本地指派成功，已通知网格员' + (res.mock ? '（演示数据）' : ''))
        this.assignVisible = false
        // 从后端刷新列表，获取最新状态（后端未连接时保留本地状态变更）
        this.fetchList()
      } catch (err) {
        console.error(err)
        ElMessage.error((err && err.message) || '指派失败，请重试')
      } finally {
        this.assigning2 = false
      }
    },
    // 该区域无可工作的本地网格员、又不允许异地指派时，发起增员请求
    async applyDemand() {
      if (!this.assignItem) return
      this.applyingDemand = true
      try {
        const res = await applyGridDemand({ afId: this.assignItem.afId })
        const body = (res && res.data) || {}
        if (body.code === 200) {
          // 无论是“已提交”还是“该区域已有待处理请求”，均提示后端返回的消息
          ElMessage.success(body.message || '增员请求已提交')
          this.assignVisible = false
          this.fetchList()
        } else {
          ElMessage.error(body.message || '申请增员失败')
        }
      } catch (err) {
        console.error(err)
        ElMessage.error((err && err.message) || '申请增员失败')
      } finally {
        this.applyingDemand = false
      }
    },
    handleDelete(item) {
      ElMessageBox.confirm('确定要删除这条反馈吗？此操作不可恢复。', '删除确认', {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      })
        .then(async () => {
          try {
            const res = await deleteByAfid(item.afId)
            if (res.data.code === 200) {
              ElMessage.success('删除成功')
              this.fetchList()
            } else {
              ElMessage.error('删除失败')
            }
          } catch (err) {
            ElMessage.error('删除请求出错')
          }
        })
        .catch(() => {})
    }
  }
}
</script>

<style scoped>
/* 查询条件条 */
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

.f-date {
  width: 250px;
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

.pre-wrap {
  white-space: pre-wrap;
  word-break: break-all;
}

/* 指派弹窗 */
.assign-target {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #f0fdf5;
  border: 1px solid #bbf7d0;
  color: #047857;
  border-radius: 10px;
  padding: 11px 14px;
  margin-bottom: 14px;
  font-size: 13.5px;
  flex-wrap: wrap;
}

.assign-addr {
  color: #64748b;
  font-weight: 400;
}

.worker-list {
  max-height: 320px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.worker-item {
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1.5px solid var(--nep-border);
  border-radius: 12px;
  padding: 11px 14px;
  cursor: pointer;
  transition: all 0.18s;
}

.worker-item:hover:not(.disabled) {
  border-color: #6ee7b7;
}

.worker-item.selected {
  border-color: #10b981;
  background: #f0fdf5;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.12);
}

.worker-item.disabled {
  opacity: 0.55;
  cursor: not-allowed;
  background: #fafafa;
}

.worker-radio {
  accent-color: #10b981;
  width: 16px;
  height: 16px;
}

.worker-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #d1fae5, #a7f3d0);
  color: #047857;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.worker-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
}

.worker-name {
  font-size: 13.5px;
  font-weight: 600;
  color: #0f172a;
}

.worker-name code {
  font-size: 11px;
  color: #94a3b8;
  background: #f1f5f9;
  border-radius: 5px;
  padding: 1px 6px;
  margin-left: 6px;
}

.worker-region {
  font-size: 12px;
  color: #94a3b8;
}

.local-tag {
  background: #10b981;
  color: #fff;
  font-size: 11px;
  border-radius: 999px;
  padding: 2px 9px;
  font-weight: 600;
}
</style>
