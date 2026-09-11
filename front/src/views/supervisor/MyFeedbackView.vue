<template>
  <div class="app-page">
    <div class="app-card">
      <div class="app-card-head">
        <div>
          <h2 class="app-card-title">
            <span class="app-ico"><i class="fa-solid fa-clock-rotate-left"></i></span>
            历史反馈
          </h2>
          <p class="app-card-sub">仅显示您本人提交的反馈，按提交时间倒序</p>
        </div>
        <button class="app-btn-primary app-btn-sm" @click="$router.push('/sf/submit')">
          <i class="fa-solid fa-plus"></i>新增反馈
        </button>
      </div>

      <div v-if="loading" class="loading-wrap" v-loading="loading" element-loading-text="数据加载中..."></div>

      <div v-else-if="list.length" class="feed-list">
        <div v-for="item in list" :key="item.afId" class="feed-card" @click="showDetail(item)">
          <div class="feed-top">
            <GradeTag :grade="item.estimatedGrade" />
            <StateTag :state="item.state" />
          </div>
          <div class="feed-loc">
            <i class="fa-solid fa-location-dot"></i>
            {{ item.provinceName || '省份' + item.provinceId }} · {{ item.cityName || '城市' + item.cityId }}
          </div>
          <div class="feed-addr">{{ item.address }}</div>
          <div class="feed-foot">
            <span><i class="fa-regular fa-calendar"></i> {{ item.afDate }} {{ item.afTime }}</span>
            <span v-if="item.aqiGrade != null" class="feed-measured">
              实测结果 · <b>{{ gradeShort(item.aqiGrade) }}</b>
            </span>
            <span v-else class="feed-waiting">等待实测</span>
            <i class="fa-solid fa-angle-right feed-arrow"></i>
          </div>
        </div>
      </div>

      <div v-else class="app-empty">
        <span class="app-empty-icon"><i class="fa-regular fa-folder-open"></i></span>
        <p>暂无反馈记录</p>
        <button class="app-btn-primary app-btn-sm" @click="$router.push('/sf/submit')">去提交第一条反馈</button>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="dialogVisible" class="app-dialog" width="440px" title="反馈详情">
      <div v-if="detail" class="detail-list">
        <div class="detail-item">
          <span class="detail-label">反馈编号</span>
          <span class="detail-value">{{ detail.afId }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">网格区域</span>
          <span class="detail-value">{{ detail.provinceName || detail.provinceId }} · {{ detail.cityName || detail.cityId }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">具体地址</span>
          <span class="detail-value">{{ detail.address }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">提交时间</span>
          <span class="detail-value">{{ detail.afDate }} {{ detail.afTime }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">预估等级</span>
          <span class="detail-value"><GradeTag :grade="detail.estimatedGrade" /></span>
        </div>
        <div class="detail-item" v-if="detail.aqiGrade != null">
          <span class="detail-label">实测等级</span>
          <span class="detail-value"><GradeTag :grade="detail.aqiGrade" /></span>
        </div>
        <div class="detail-item" v-if="detail.aqiGrade != null">
          <span class="detail-label">实测明细</span>
          <span class="detail-value">SO₂ {{ detail.so2Grade }}级 · CO {{ detail.coGrade }}级 · PM2.5 {{ detail.pm25Grade }}级</span>
        </div>
        <div class="detail-item" v-if="detail.aqiGrade != null">
          <span class="detail-label">实测网格员</span>
          <span class="detail-value">{{ detail.gridName || detail.gridCode }}</span>
        </div>
        <div class="detail-item" v-if="detail.aqiGrade != null">
          <span class="detail-label">实测时间</span>
          <span class="detail-value">{{ detail.measureDate }} {{ detail.measureTime }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">当前状态</span>
          <span class="detail-value"><StateTag :state="detail.state" /></span>
        </div>
        <div class="detail-item detail-info">
          <span class="detail-label">反馈信息</span>
          <pre class="detail-value pre-wrap">{{ detail.information }}</pre>
        </div>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import GradeTag from '../../components/GradeTag.vue'
import StateTag from '../../components/StateTag.vue'
import { getAqiFeedbackList } from '../../api/aqiFeedback'
import { gradeShort } from '../../constants/aqi'

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
    gradeShort,
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
.loading-wrap {
  min-height: 260px;
}

.feed-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 14px;
}

@media (max-width: 768px) {
  .feed-list {
    grid-template-columns: 1fr;
  }
}

.feed-card {
  background: #fff;
  border: 1px solid var(--app-ring);
  border-radius: 18px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.feed-card:hover {
  border-color: rgba(16, 185, 129, 0.4);
  box-shadow: 0 8px 22px rgba(4, 120, 87, 0.10);
  transform: translateY(-1px);
}

.feed-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.feed-loc {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14.5px;
  font-weight: 700;
  color: #0f172a;
}

.feed-loc i {
  color: var(--app-strong);
  font-size: 13px;
}

.feed-addr {
  margin: 4px 0 0 19px;
  font-size: 13px;
  color: #64748b;
}

.feed-foot {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 12px;
  font-size: 12px;
  color: #94a3b8;
  flex-wrap: wrap;
}

.feed-foot i {
  margin-right: 3px;
}

.feed-measured {
  color: var(--app-strong);
  font-weight: 600;
}

.feed-waiting {
  color: #b45309;
  background: #fffbeb;
  padding: 3px 10px;
  border-radius: 999px;
}

.feed-arrow {
  margin-left: auto;
  color: #cbd5e1;
  font-size: 13px;
}

/* 详情 */
.detail-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-item {
  display: flex;
  gap: 12px;
  font-size: 13.5px;
}

.detail-label {
  width: 76px;
  flex-shrink: 0;
  color: #94a3b8;
}

.detail-value {
  flex: 1;
  color: #334155;
}

.detail-info .detail-value {
  background: #f8fafc;
  border-radius: 12px;
  padding: 10px 12px;
  font-family: inherit;
  font-size: 13.5px;
}

.pre-wrap {
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
