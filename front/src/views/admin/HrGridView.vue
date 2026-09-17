<template>
  <div class="nep-page">
    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <!-- ============ 网格员管理 ============ -->
      <el-tab-pane label="网格员管理" name="grid">
        <div class="nep-card">
          <div class="nep-card-header">
            <div>
              <h2 class="nep-card-title">
                <span class="nep-title-icon"><i class="fa-solid fa-user-gear"></i></span>
                网格员管理
              </h2>
              <p class="nep-card-sub">注册网格员账号、录入信息、维护负责地区与工作状态</p>
            </div>
            <div class="header-actions">
              <el-button type="primary" class="nep-btn-gradient" @click="openCreate">
                <i class="fa-solid fa-user-plus" style="margin-right:6px"></i>新增网格员
              </el-button>
              <el-button :icon="Refresh" circle @click="fetchEmployees" />
            </div>
          </div>

          <div class="nep-table-wrap">
            <el-table v-loading="loading" :data="employees" style="width: 100%"
              :header-cell-style="{ background: '#f8faf9' }">
              <el-table-column prop="empCode" label="登录编码" width="110" />
              <el-table-column prop="realName" label="姓名" width="110" />
              <el-table-column label="负责地区" min-width="180">
                <template #default="{ row }">
                  <span class="region-text">{{ row.provinceName || '未分配' }}<template v-if="row.cityName"> · {{ row.cityName }}</template></span>
                </template>
              </el-table-column>
              <el-table-column label="工作状态" width="110" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.working ? 'success' : 'danger'" effect="light">
                    {{ row.working ? '在职' : '请假中' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="200" align="center">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
                  <el-button link type="primary" @click="openLeaves(row)">请假记录</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-tab-pane>

      <!-- ============ 请假审批 ============ -->
      <el-tab-pane label="请假审批" name="leave">
        <div class="nep-card">
          <div class="nep-card-header">
            <div>
              <h2 class="nep-card-title">
                <span class="nep-title-icon"><i class="fa-solid fa-calendar-check"></i></span>
                请假审批
              </h2>
              <p class="nep-card-sub">审批网格员请假；同意后该网格员进入请假（非工作）状态，无法登录与接单</p>
            </div>
            <div class="header-actions">
              <el-select v-model="leaveStateFilter" placeholder="审批状态" clearable style="width: 140px"
                @change="fetchLeaves">
                <el-option v-for="s in leaveStateOptions" :key="s.value" :label="s.label" :value="s.value" />
              </el-select>
              <el-button :icon="Refresh" circle @click="fetchLeaves" />
            </div>
          </div>

          <div class="nep-table-wrap">
            <el-table v-loading="leaveLoading" :data="leaves" style="width: 100%"
              :header-cell-style="{ background: '#f8faf9' }">
              <el-table-column prop="gridName" label="网格员" width="100" />
              <el-table-column prop="empCode" label="登录编码" width="100" />
              <el-table-column prop="reason" label="请假事由" min-width="180" show-overflow-tooltip />
              <el-table-column label="请假期间" width="190">
                <template #default="{ row }">{{ row.startDate }} ~ {{ row.endDate }}</template>
              </el-table-column>
              <el-table-column label="申请时间" width="165">
                <template #default="{ row }">{{ row.applyDate }} {{ row.applyTime }}</template>
              </el-table-column>
              <el-table-column label="审批时间" width="165">
                <template #default="{ row }">
                  <span v-if="row.approveDate" class="muted">{{ row.approveDate }} {{ row.approveTime }}</span>
                  <span v-else class="muted">—</span>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="120" align="center">
                <template #default="{ row }">
                  <el-tag :type="leaveTagType(row.state)" effect="light">{{ leaveStateText(row.state) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="150" align="center">
                <template #default="{ row }">
                  <template v-if="row.state === 0">
                    <el-button link type="success" @click="handleApprove(row, true)">同意</el-button>
                    <el-button link type="danger" @click="handleApprove(row, false)">驳回</el-button>
                  </template>
                  <el-button v-else-if="row.state === 1" link type="primary" @click="handleBack(row)">销假</el-button>
                  <span v-else class="muted">—</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-tab-pane>

      <!-- ============ 增员请求 ============ -->
      <el-tab-pane label="增员请求" name="demand">
        <div class="nep-card">
          <div class="nep-card-header">
            <div>
              <h2 class="nep-card-title">
                <span class="nep-title-icon"><i class="fa-solid fa-user-plus"></i></span>
                增员请求
              </h2>
              <p class="nep-card-sub">网格区域没有可工作的本地网格员、又不允许异地指派时由管理员发起；处理结果作为决策者判断是否需要增员的依据</p>
            </div>
            <div class="header-actions">
              <el-select v-model="demandStateFilter" placeholder="处理状态" clearable style="width: 140px"
                @change="fetchDemands">
                <el-option v-for="s in demandStateOptions" :key="s.value" :label="s.label" :value="s.value" />
              </el-select>
              <el-button :icon="Refresh" circle @click="fetchDemands" />
            </div>
          </div>

          <div class="nep-table-wrap">
            <el-table v-loading="demandLoading" :data="demands" style="width: 100%"
              :header-cell-style="{ background: '#f8faf9' }">
              <el-table-column label="省份 / 城市" min-width="150">
                <template #default="{ row }">{{ row.provinceName }} · {{ row.cityName }}</template>
              </el-table-column>
              <el-table-column label="来源反馈" width="100" align="center">
                <template #default="{ row }">
                  <span v-if="row.afId">{{ row.afId }}</span>
                  <span v-else class="muted">—</span>
                </template>
              </el-table-column>
              <el-table-column prop="reason" label="缺员说明" min-width="220" show-overflow-tooltip />
              <el-table-column label="申请时间" width="165">
                <template #default="{ row }">{{ row.applyDate }} {{ row.applyTime }}</template>
              </el-table-column>
              <el-table-column label="状态" width="110" align="center">
                <template #default="{ row }">
                  <el-tag :type="demandTagType(row.state)" effect="light">{{ demandStateText(row.state) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="处理时间" width="165">
                <template #default="{ row }">
                  <span v-if="row.handleDate" class="muted">{{ row.handleDate }} {{ row.handleTime }}</span>
                  <span v-else class="muted">—</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="150" align="center">
                <template #default="{ row }">
                  <template v-if="row.state === 0">
                    <el-button link type="success" @click="handleDemand(row, 1)">已处理</el-button>
                    <el-button link type="danger" @click="handleDemand(row, 2)">忽略</el-button>
                  </template>
                  <span v-else class="muted">—</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 新增 / 编辑网格员 -->
    <el-dialog v-model="editVisible" :title="editForm.empId ? '编辑网格员' : '新增网格员'" width="480px">
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="登录编码" required>
          <el-input v-model="editForm.empCode" :disabled="!!editForm.empId" placeholder="如 grid011" />
        </el-form-item>
        <el-form-item label="姓名" required>
          <el-input v-model="editForm.realName" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item v-if="!editForm.empId" label="初始密码" required>
          <el-input v-model="editForm.password" type="password" show-password placeholder="不少于6位" />
        </el-form-item>
        <el-form-item label="负责省份" required>
          <el-select v-model="editForm.provinceId" placeholder="选择省份" style="width: 100%"
            @change="onProvinceChange">
            <el-option v-for="p in provinces" :key="p.provinceId" :label="p.provinceName" :value="p.provinceId" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责城市">
          <el-select v-model="editForm.cityId" placeholder="选择城市" style="width: 100%"
            :disabled="!editForm.provinceId" clearable>
            <el-option v-for="c in cities" :key="c.cityId" :label="c.cityName" :value="c.cityId" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="editForm.empId" label="工作状态">
          <el-radio-group v-model="editForm.working">
            <el-radio :value="1">在职</el-radio>
            <el-radio :value="0">请假中</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" class="nep-btn-gradient" :loading="saving" @click="saveEmployee">保存</el-button>
      </template>
    </el-dialog>

    <!-- 请假记录抽屉 -->
    <el-drawer v-model="leavesVisible" :title="'请假记录 · ' + (currentEmp ? currentEmp.realName : '')" size="560px">
      <el-table v-loading="empLeaveLoading" :data="empLeaves" style="width: 100%"
        :header-cell-style="{ background: '#f8faf9' }">
        <el-table-column prop="reason" label="事由" min-width="120" show-overflow-tooltip />
        <el-table-column label="请假期间" width="180">
          <template #default="{ row }">{{ row.startDate }} ~ {{ row.endDate }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="leaveTagType(row.state)" effect="light">{{ leaveStateText(row.state) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="160">
          <template #default="{ row }">{{ row.applyDate }} {{ row.applyTime }}</template>
        </el-table-column>
      </el-table>
    </el-drawer>
  </div>
</template>

<script>
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getProvinces, getCityByPid } from '../../api/aqiFeedback'
import {
  getEmployeeList, saveEmployee, updateEmployee,
  getLeaveList, approveLeave, backLeave
} from '../../api/hr'
import { getGridDemandList, handleGridDemand } from '../../api/gridDemand'

// 请假状态：label / el-tag type
const LEAVE_STATES = {
  0: ['待审批', 'warning'],
  1: ['已同意·请假中', 'danger'],
  2: ['已驳回', 'info'],
  3: ['已销假', 'success']
}

// 增员请求状态：label / el-tag type
const DEMAND_STATES = {
  0: ['待处理', 'warning'],
  1: ['已处理', 'success'],
  2: ['已忽略', 'info']
}

export default {
  name: 'HrGridView',
  data() {
    return {
      activeTab: 'grid',
      loading: false,
      employees: [],
      provinces: [],
      cities: [],
      editVisible: false,
      saving: false,
      editForm: {},
      leavesVisible: false,
      currentEmp: null,
      empLeaves: [],
      empLeaveLoading: false,
      leaveLoading: false,
      leaves: [],
      leaveStateFilter: null,
      demandLoading: false,
      demands: [],
      demandStateFilter: null
    }
  },
  computed: {
    leaveStateOptions() {
      return Object.entries(LEAVE_STATES).map(([value, [label]]) => ({ value: Number(value), label }))
    },
    demandStateOptions() {
      return Object.entries(DEMAND_STATES).map(([value, [label]]) => ({ value: Number(value), label }))
    }
  },
  created() {
    this.fetchEmployees()
    this.fetchLeaves()
    this.fetchDemands()
    getProvinces().then(res => {
      if (res.data.code === 200) this.provinces = res.data.data
    }).catch(() => {})
  },
  methods: {
    leaveStateText(state) {
      const item = LEAVE_STATES[state]
      return item ? item[0] : '未知'
    },
    leaveTagType(state) {
      const item = LEAVE_STATES[state]
      return item ? item[1] : 'info'
    },
    demandStateText(state) {
      const item = DEMAND_STATES[state]
      return item ? item[0] : '未知'
    },
    demandTagType(state) {
      const item = DEMAND_STATES[state]
      return item ? item[1] : 'info'
    },
    async fetchEmployees() {
      this.loading = true
      try {
        const res = await getEmployeeList()
        if (res.data.code === 200) {
          this.employees = res.data.data
        } else {
          ElMessage.error(res.data.message || '获取网格员列表失败')
        }
      } catch (err) {
        console.error(err)
        ElMessage.error('网络异常，请确认后端服务已启动')
      } finally {
        this.loading = false
      }
    },
    async fetchLeaves() {
      this.leaveLoading = true
      try {
        const params = this.leaveStateFilter != null ? { state: this.leaveStateFilter } : {}
        const res = await getLeaveList(params)
        if (res.data.code === 200) {
          this.leaves = res.data.data
        } else {
          ElMessage.error(res.data.message || '获取请假记录失败')
        }
      } catch (err) {
        console.error(err)
        ElMessage.error('网络异常，请确认后端服务已启动')
      } finally {
        this.leaveLoading = false
      }
    },
    // 增员请求列表（可按处理状态筛选）
    async fetchDemands() {
      this.demandLoading = true
      try {
        const params = this.demandStateFilter != null ? { state: this.demandStateFilter } : {}
        const res = await getGridDemandList(params)
        if (res.data.code === 200) {
          this.demands = res.data.data || []
        } else {
          ElMessage.error(res.data.message || '获取增员请求失败')
        }
      } catch (err) {
        console.error(err)
        ElMessage.error('网络异常，请确认后端服务已启动')
      } finally {
        this.demandLoading = false
      }
    },
    // 切到“增员请求”页签时刷新，保证看到最新的待处理请求
    onTabChange(name) {
      if (name === 'demand') this.fetchDemands()
    },
    // 处理增员请求：1 已处理（已增员） / 2 已忽略
    handleDemand(row, state) {
      const action = state === 1 ? '已处理' : '忽略'
      ElMessageBox.confirm(
        `确定将「${row.provinceName} · ${row.cityName}」的增员请求标记为「${action}」吗？`,
        '处理增员请求',
        { confirmButtonText: action, cancelButtonText: '取消', type: 'warning' }
      ).then(async () => {
        try {
          const res = await handleGridDemand({ demandId: row.demandId, state })
          if (res.data.code === 200) {
            ElMessage.success(res.data.message || '处理完成')
            this.fetchDemands()
          } else {
            ElMessage.error(res.data.message || '处理失败')
          }
        } catch (err) {
          console.error(err)
          ElMessage.error('网络异常，请确认后端服务已启动')
        }
      }).catch(() => {})
    },
    openCreate() {
      this.editForm = { empCode: '', realName: '', password: '', provinceId: null, cityId: null }
      this.cities = []
      this.editVisible = true
    },
    openEdit(row) {
      this.editForm = {
        empId: row.empId,
        empCode: row.empCode,
        realName: row.realName,
        provinceId: row.provinceId,
        cityId: row.cityId,
        working: row.working
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
    async saveEmployee() {
      const f = this.editForm
      if (!f.empCode || !f.realName || !f.provinceId) {
        ElMessage.warning('请填写登录编码、姓名与负责省份')
        return
      }
      if (!f.empId && (!f.password || f.password.length < 6)) {
        ElMessage.warning('初始密码不能少于6位')
        return
      }
      this.saving = true
      try {
        const res = f.empId
          ? await updateEmployee({ empId: f.empId, realName: f.realName, provinceId: f.provinceId, cityId: f.cityId, working: f.working })
          : await saveEmployee({ empCode: f.empCode, realName: f.realName, password: f.password, provinceId: f.provinceId, cityId: f.cityId, working: 1 })
        if (res.data.code === 200) {
          ElMessage.success(res.data.message || '保存成功')
          this.editVisible = false
          this.fetchEmployees()
        } else {
          ElMessage.error(res.data.message || '保存失败')
        }
      } catch (err) {
        console.error(err)
        ElMessage.error('网络异常，请确认后端服务已启动')
      } finally {
        this.saving = false
      }
    },
    async openLeaves(row) {
      this.currentEmp = row
      this.leavesVisible = true
      this.empLeaveLoading = true
      this.empLeaves = []
      try {
        const res = await getLeaveList({ empCode: row.empCode })
        if (res.data.code === 200) this.empLeaves = res.data.data
      } catch (err) {
        console.error(err)
        ElMessage.error('网络异常，请确认后端服务已启动')
      } finally {
        this.empLeaveLoading = false
      }
    },
    handleApprove(row, agree) {
      const action = agree ? '同意' : '驳回'
      ElMessageBox.confirm(`确定${action}「${row.gridName}」的请假申请吗？`, '审批请假', {
        confirmButtonText: action,
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const res = await approveLeave({ leaveId: row.leaveId, agree })
          if (res.data.code === 200) {
            ElMessage.success(res.data.message || '审批完成')
            this.fetchLeaves()
            this.fetchEmployees()
          } else {
            ElMessage.error(res.data.message || '审批失败')
          }
        } catch (err) {
          console.error(err)
          ElMessage.error('网络异常，请确认后端服务已启动')
        }
      }).catch(() => {})
    },
    handleBack(row) {
      ElMessageBox.confirm(`确定对「${row.gridName}」销假，恢复其工作状态吗？`, '销假', {
        confirmButtonText: '销假',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const res = await backLeave({ leaveId: row.leaveId })
          if (res.data.code === 200) {
            ElMessage.success(res.data.message || '销假成功')
            this.fetchLeaves()
            this.fetchEmployees()
          } else {
            ElMessage.error(res.data.message || '销假失败')
          }
        } catch (err) {
          console.error(err)
          ElMessage.error('网络异常，请确认后端服务已启动')
        }
      }).catch(() => {})
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

.muted {
  color: #94a3b8;
  font-size: 13px;
}
</style>
