<template>
  <div class="nep-page">
    <div class="nep-card">
      <div class="nep-card-header">
        <div>
          <h2 class="nep-card-title">
            <span class="nep-title-icon"><i class="fa-solid fa-calendar-days"></i></span>
            请假申请
          </h2>
          <p class="nep-card-sub">请假须经管理员审批，同意后进入请假（非工作）状态，期间无法接收新任务</p>
        </div>
      </div>

      <el-form :model="form" label-width="80px" class="leave-form">
        <el-form-item label="请假事由" required>
          <el-input v-model="form.reason" type="textarea" :rows="3" maxlength="200" show-word-limit
            placeholder="请填写请假原因（如：身体不适、家中事务等）" />
        </el-form-item>
        <el-form-item label="请假期间" required>
          <el-date-picker v-model="dateRange" type="daterange" range-separator="至"
            start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 320px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="nep-btn-gradient" :loading="submitting" @click="submitApply">
            <i class="fa-solid fa-paper-plane" style="margin-right:6px"></i>提交申请
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="nep-card">
      <div class="nep-card-header">
        <div>
          <h2 class="nep-card-title">
            <span class="nep-title-icon"><i class="fa-solid fa-clock-rotate-left"></i></span>
            我的请假记录
          </h2>
          <p class="nep-card-sub">按申请时间倒序展示本人的请假与审批情况</p>
        </div>
        <el-button :icon="Refresh" circle @click="fetchList" />
      </div>

      <div class="nep-table-wrap">
        <el-table v-loading="loading" :data="list" style="width: 100%"
          :header-cell-style="{ background: '#f8faf9' }">
          <el-table-column prop="reason" label="请假事由" min-width="160" show-overflow-tooltip />
          <el-table-column label="请假期间" width="190">
            <template #default="{ row }">{{ row.startDate }} ~ {{ row.endDate }}</template>
          </el-table-column>
          <el-table-column label="申请时间" width="165">
            <template #default="{ row }">{{ row.applyDate }} {{ row.applyTime }}</template>
          </el-table-column>
          <el-table-column label="审批时间" width="165">
            <template #default="{ row }">
              <span v-if="row.approveDate">{{ row.approveDate }} {{ row.approveTime }}</span>
              <span v-else class="muted">—</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="130" align="center">
            <template #default="{ row }">
              <el-tag :type="leaveTagType(row.state)" effect="light">{{ leaveStateText(row.state) }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="!loading && !list.length" class="nep-empty">
          <span class="nep-empty-icon"><i class="fa-regular fa-folder-open"></i></span>
          <span>暂无请假记录</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { applyLeave, getLeaveList } from '../../api/hr'

const LEAVE_STATES = {
  0: ['待审批', 'warning'],
  1: ['已同意·请假中', 'danger'],
  2: ['已驳回', 'info'],
  3: ['已销假', 'success']
}

export default {
  name: 'LeaveView',
  data() {
    return {
      form: { reason: '' },
      dateRange: null,
      submitting: false,
      loading: false,
      list: []
    }
  },
  created() {
    this.fetchList()
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
    async fetchList() {
      this.loading = true
      try {
        const res = await getLeaveList({ empCode: this.$store.state.user.account })
        if (res.data.code === 200) {
          this.list = res.data.data
        } else {
          ElMessage.error(res.data.message || '获取请假记录失败')
        }
      } catch (err) {
        console.error(err)
        ElMessage.error('网络异常，请确认后端服务已启动')
      } finally {
        this.loading = false
      }
    },
    async submitApply() {
      if (!this.form.reason || !this.form.reason.trim()) {
        ElMessage.warning('请填写请假事由')
        return
      }
      if (!this.dateRange || !this.dateRange.length) {
        ElMessage.warning('请选择请假期间')
        return
      }
      this.submitting = true
      try {
        const res = await applyLeave({
          empCode: this.$store.state.user.account,
          reason: this.form.reason.trim(),
          startDate: this.dateRange[0],
          endDate: this.dateRange[1]
        })
        if (res.data.code === 200) {
          ElMessage.success(res.data.message || '请假申请已提交')
          this.form.reason = ''
          this.dateRange = null
          this.fetchList()
        } else {
          ElMessage.error(res.data.message || '提交失败')
        }
      } catch (err) {
        console.error(err)
        ElMessage.error('网络异常，请确认后端服务已启动')
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style scoped>
.leave-form {
  max-width: 560px;
}

.muted {
  color: #94a3b8;
  font-size: 13px;
}
</style>
