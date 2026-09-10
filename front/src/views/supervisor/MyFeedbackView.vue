<template>
  <div class="nep-page">
    <div class="nep-card">
      <div class="nep-card-header">
        <div>
          <h2 class="nep-card-title">
            <span class="nep-title-icon"><i class="fa-solid fa-clock-rotate-left"></i></span>
            历史反馈信息
          </h2>
          <p class="nep-card-sub">仅显示您本人提交的反馈，按提交时间倒序</p>
        </div>
        <el-button type="primary" class="nep-btn-gradient" @click="$router.push('/sf/submit')">
          <i class="fa-solid fa-plus" style="margin-right:6px"></i>新增反馈
        </el-button>
      </div>

      <div v-if="loading" class="loading-wrap" v-loading="loading" element-loading-text="数据加载中..."></div>

      <div v-else-if="list.length" class="nep-table-wrap">
        <el-table :data="list" style="width: 100%" :header-cell-style="{ background: '#f8faf9' }"
          @row-click="showDetail">
          <el-table-column label="网格区域" min-width="170">
            <template #default="{ row }">
              <span class="region"><i class="fa-solid fa-location-dot region-icon"></i>
                {{ row.provinceName || '省份' + row.provinceId }} · {{ row.cityName || '城市' + row.cityId }}</span>
            </template>
          </el-table-column>
          <el-table-column label="具体地址" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="muted">{{ row.address }}</span>
            </template>
          </el-table-column>
          <el-table-column label="提交时间" width="170">
            <template #default="{ row }">
              <span class="muted">{{ row.afDate }} {{ row.afTime }}</span>
            </template>
          </el-table-column>
          <el-table-column label="预估AQI等级" width="130" align="center">
            <template #default="{ row }">
              <GradeTag :grade="row.estimatedGrade" />
            </template>
          </el-table-column>
          <el-table-column label="状态" width="95" align="center">
            <template #default="{ row }">
              <StateTag :state="row.state" />
            </template>
          </el-table-column>
          <el-table-column label="" width="50" align="center">
            <template #default>
              <i class="fa-solid fa-angle-right row-arrow"></i>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div v-else class="nep-empty">
        <span class="nep-empty-icon"><i class="fa-regular fa-folder-open"></i></span>
        <span>暂无反馈记录</span>
        <el-button text type="primary" @click="$router.push('/sf/submit')">去提交第一条反馈</el-button>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="dialogVisible" title="反馈详情" width="560px">
      <el-descriptions :column="1" border v-if="detail">
        <el-descriptions-item label="反馈编号">{{ detail.afId }}</el-descriptions-item>
        <el-descriptions-item label="网格区域">
          {{ detail.provinceName || detail.provinceId }} · {{ detail.cityName || detail.cityId }}
        </el-descriptions-item>
        <el-descriptions-item label="具体地址">{{ detail.address }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ detail.afDate }} {{ detail.afTime }}</el-descriptions-item>
        <el-descriptions-item label="预估等级">
          <GradeTag :grade="detail.estimatedGrade" />
        </el-descriptions-item>
        <el-descriptions-item label="当前状态">
          <StateTag :state="detail.state" />
        </el-descriptions-item>
        <el-descriptions-item label="反馈信息">
          <span class="pre-wrap">{{ detail.information }}</span>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import GradeTag from '../../components/GradeTag.vue'
import StateTag from '../../components/StateTag.vue'
import { getAqiFeedbackList } from '../../api/aqiFeedback'

export default {
  name: 'MyFeedbackView',
  components: { GradeTag, StateTag },
  data() {
    return {
      list: [],
      loading: true,
      detail: null,
      dialogVisible: false
    }
  },
  computed: {
    myAccount() {
      return this.$store.state.user ? this.$store.state.user.account : ''
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    async fetchList() {
      this.loading = true
      try {
        const res = await getAqiFeedbackList()
        if (res.data.code === 200) {
          const all = res.data.data || []
          // 仅显示本人反馈（手机号即身份识别）
          this.list = all
            .filter(f => f.telId === this.myAccount)
            .sort((a, b) => (b.afDate + (b.afTime || '')).localeCompare(a.afDate + (a.afTime || '')))
        } else {
          ElMessage.error('获取列表失败')
        }
      } catch (err) {
        console.error(err)
        ElMessage.error('网络异常，请确认后端服务已启动')
      } finally {
        this.loading = false
      }
    },
    showDetail(item) {
      this.detail = item
      this.dialogVisible = true
    }
  }
}
</script>

<style scoped>
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

.row-arrow {
  color: #cbd5e1;
}

:deep(.el-table__row) {
  cursor: pointer;
}

.pre-wrap {
  white-space: pre-wrap;
  word-break: break-all;
}

.loading-wrap {
  min-height: 260px;
}
</style>
