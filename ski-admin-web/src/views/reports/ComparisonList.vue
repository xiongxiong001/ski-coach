<template>
  <div class="page-container">
    <!-- 筛选 -->
    <div class="card filter-card">
      <el-form :inline="true" :model="filters" @submit.prevent="handleSearch">
        <el-form-item label="用户ID">
          <el-input
            v-model="filters.userId"
            placeholder="精确匹配用户ID"
            clearable
            style="width: 160px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input
            v-model="filters.userPhone"
            placeholder="模糊搜索手机号"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button :icon="Refresh" @click="loadList">刷新</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 列表 -->
    <div class="card list-card">
      <div class="list-header">
        <div>
          <div class="list-title">对比报告</div>
          <div class="list-sub">所有用户的进步对比报告 · 杀手级差异化能力的核心产出</div>
        </div>
        <div class="list-meta">共 {{ total }} 份</div>
      </div>

      <el-table
        v-loading="loading"
        :data="list"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="用户" min-width="180">
          <template #default="{ row }">
            <div class="user-cell">
              <div class="user-name">{{ row.userNickname || '(未设置昵称)' }}</div>
              <div class="user-phone">{{ row.userPhone }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="对比视频" width="160" align="center">
          <template #default="{ row }">
            <span class="vs-badge">
              #{{ row.prevVideoId }} ↔ #{{ row.currVideoId }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="进步统计" width="200">
          <template #default="{ row }">
            <div class="metrics-cell">
              <span class="metric improved">📈 {{ row.improvedCount || 0 }}</span>
              <span class="metric declined">📉 {{ row.declinedCount || 0 }}</span>
              <span class="metric stable">🛡️ {{ row.stabilityImprovedCount || 0 }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="报告摘要" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="preview-text">{{ row.reportPreview }}</span>
          </template>
        </el-table-column>
        <el-table-column label="LLM 花费" width="100" align="right">
          <template #default="{ row }">
            <span class="cost-cell">{{ formatYuan(row.llmCostYuan) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="生成时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.createdTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="filters.pageNum"
          v-model:page-size="filters.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          background
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </div>

    <!-- 详情抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      title="对比报告详情"
      direction="rtl"
      size="700"
    >
      <div v-loading="detailLoading" class="report-detail">
        <div v-if="detail">
          <!-- 元信息 -->
          <div class="meta-card">
            <div class="meta-row">
              <div class="meta-item">
                <div class="meta-label">报告 ID</div>
                <div class="meta-value">#{{ detail.id }}</div>
              </div>
              <div class="meta-item">
                <div class="meta-label">用户</div>
                <div class="meta-value">{{ detail.userNickname || '-' }}</div>
                <div class="meta-sub">{{ detail.userPhone }}</div>
              </div>
              <div class="meta-item">
                <div class="meta-label">生成时间</div>
                <div class="meta-value time-value">{{ formatDateTime(detail.createdTime) }}</div>
              </div>
            </div>
          </div>

          <!-- 视频对(VS 卡) -->
          <div class="vs-card">
            <div class="vs-side">
              <div class="vs-tag prev">上次</div>
              <div class="vs-name">{{ detail.prevVideoFilename || '(已删除)' }}</div>
              <div class="vs-id">#{{ detail.prevVideoId }}</div>
            </div>
            <div class="vs-vs">VS</div>
            <div class="vs-side">
              <div class="vs-tag curr">本次</div>
              <div class="vs-name">{{ detail.currVideoFilename || '(已删除)' }}</div>
              <div class="vs-id">#{{ detail.currVideoId }}</div>
            </div>
          </div>

          <!-- 进步统计 -->
          <div class="metrics-card">
            <div class="metric-block improved">
              <div class="metric-icon">📈</div>
              <div class="metric-num">{{ detail.improvedCount || 0 }}</div>
              <div class="metric-label">进步项</div>
            </div>
            <div class="metric-block declined">
              <div class="metric-icon">📉</div>
              <div class="metric-num">{{ detail.declinedCount || 0 }}</div>
              <div class="metric-label">退步项</div>
            </div>
            <div class="metric-block stable">
              <div class="metric-icon">🛡️</div>
              <div class="metric-num">{{ detail.stabilityImprovedCount || 0 }}</div>
              <div class="metric-label">更稳定</div>
            </div>
          </div>

          <!-- 成本 -->
          <div class="meta-card cost-card">
            <div class="card-section-title">LLM 调用成本</div>
            <div class="meta-row">
              <div class="meta-item">
                <div class="meta-label">花费</div>
                <div class="meta-value cost-value">{{ formatYuan(detail.llmCostYuan) }}</div>
              </div>
              <div class="meta-item">
                <div class="meta-label">输入 tokens</div>
                <div class="meta-value">{{ formatNumber(detail.llmInputTokens) }}</div>
              </div>
              <div class="meta-item">
                <div class="meta-label">输出 tokens</div>
                <div class="meta-value">{{ formatNumber(detail.llmOutputTokens) }}</div>
              </div>
            </div>
          </div>

          <!-- 完整报告 -->
          <div class="report-body">
            <div class="card-section-title">教练对比报告全文</div>
            <MarkdownView :content="detail.reportMarkdown" />
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { listComparisons, getComparisonDetail } from '@/api/reports'
import { formatDateTime, formatYuan, formatNumber } from '@/utils/format'
import MarkdownView from '@/components/MarkdownView.vue'

const list = ref([])
const total = ref(0)
const loading = ref(false)

const filters = reactive({
  pageNum: 1,
  pageSize: 20,
  userId: '',
  userPhone: ''
})

const drawerVisible = ref(false)
const detail = ref(null)
const detailLoading = ref(false)

onMounted(() => loadList())

async function loadList() {
  loading.value = true
  try {
    const params = { ...filters }
    Object.keys(params).forEach(k => {
      if (params[k] === '' || params[k] === null) delete params[k]
    })
    const data = await listComparisons(params)
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  filters.pageNum = 1
  loadList()
}

function handleReset() {
  filters.pageNum = 1
  filters.userId = ''
  filters.userPhone = ''
  loadList()
}

async function openDetail(row) {
  drawerVisible.value = true
  detailLoading.value = true
  try {
    detail.value = await getComparisonDetail(row.id)
  } finally {
    detailLoading.value = false
  }
}
</script>

<style lang="scss" scoped>
.filter-card {
  margin-bottom: $space-md;
  padding: $space-md $space-lg;

  :deep(.el-form--inline .el-form-item) { margin-bottom: 0; }
}

.list-card { padding: 0; }

.list-header {
  padding: $space-md $space-lg;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid $border-light;

  .list-title { font-size: $font-md; font-weight: 600; }
  .list-sub { font-size: $font-xs; color: $text-secondary; margin-top: 2px; }
  .list-meta { font-size: $font-sm; color: $text-secondary; }
}

.user-cell {
  .user-name { font-size: $font-sm; color: $text-primary; font-weight: 500; }
  .user-phone { font-size: $font-xs; color: $text-secondary; font-variant-numeric: tabular-nums; }
}

.vs-badge {
  display: inline-block;
  padding: 4px 10px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.1), rgba(139, 92, 246, 0.1));
  color: #6d28d9;
  border-radius: $radius-sm;
  font-size: $font-xs;
  font-variant-numeric: tabular-nums;
  font-weight: 500;
}

.metrics-cell {
  display: flex;
  gap: $space-sm;
  font-size: $font-xs;

  .metric {
    display: inline-flex;
    align-items: center;
    gap: 2px;
    font-variant-numeric: tabular-nums;

    &.improved { color: $color-success; }
    &.declined { color: $color-danger; }
    &.stable   { color: $color-primary; }
  }
}

.preview-text {
  color: $text-regular;
  font-size: $font-sm;
}

.cost-cell {
  font-variant-numeric: tabular-nums;
  color: $color-warning;
  font-weight: 500;
}

.pagination {
  padding: $space-md $space-lg;
  display: flex;
  justify-content: flex-end;
}

// 抽屉
.report-detail { padding: $space-md; }

.card-section-title {
  font-size: $font-sm;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: $space-md;
  padding-left: $space-sm;
  border-left: 3px solid $color-primary;
  line-height: 1.2;
}

.meta-card {
  background: $bg-page;
  border: 1px solid $border-light;
  border-radius: $radius-md;
  padding: $space-md;
  margin-bottom: $space-md;
}

.cost-card {
  background: #FEF3C7;
  border-color: #FCD34D;
}

.meta-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: $space-md;

  + .meta-row { margin-top: $space-md; }
}

.meta-item {
  .meta-label {
    font-size: $font-xs;
    color: $text-secondary;
    margin-bottom: 4px;
  }
  .meta-value {
    font-size: $font-sm;
    color: $text-primary;
    font-weight: 500;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;

    &.time-value { font-variant-numeric: tabular-nums; font-size: $font-xs; }
    &.cost-value { font-size: $font-md; color: $color-warning; font-weight: 700; }
  }
  .meta-sub {
    font-size: $font-xs;
    color: $text-secondary;
    margin-top: 2px;
    font-variant-numeric: tabular-nums;
  }
}

// VS 卡
.vs-card {
  display: flex;
  align-items: stretch;
  background: linear-gradient(135deg, #1e3a8a 0%, #6d28d9 50%, #be185d 100%);
  color: white;
  border-radius: $radius-md;
  padding: $space-md;
  margin-bottom: $space-md;
  gap: $space-md;
}

.vs-side {
  flex: 1;
  text-align: center;
  padding: $space-sm;
}

.vs-tag {
  display: inline-block;
  padding: 2px 10px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  border-radius: $radius-full, 999px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 600;
  margin-bottom: $space-sm;
}

.vs-name {
  font-size: $font-xs;
  font-weight: 500;
  margin-bottom: 4px;
  word-break: break-all;
  font-family: "SF Mono", Consolas, monospace;
  opacity: 0.95;
}

.vs-id {
  font-size: 10px;
  opacity: 0.7;
}

.vs-vs {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: $font-md;
  font-weight: 700;
  letter-spacing: 1px;
  width: 32px;
}

// 进步统计 3 卡
.metrics-card {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: $space-md;
  margin-bottom: $space-md;
}

.metric-block {
  text-align: center;
  padding: $space-md;
  border-radius: $radius-md;
  border: 1px solid $border-light;

  .metric-icon {
    font-size: 22px;
    margin-bottom: 4px;
  }
  .metric-num {
    font-size: 28px;
    font-weight: 700;
    line-height: 1.1;
  }
  .metric-label {
    font-size: $font-xs;
    color: $text-secondary;
    margin-top: 4px;
  }

  &.improved { background: rgba(16, 185, 129, 0.06); .metric-num { color: $color-success; } }
  &.declined { background: rgba(239, 68, 68, 0.06);  .metric-num { color: $color-danger;  } }
  &.stable   { background: rgba(59, 130, 246, 0.06); .metric-num { color: $color-primary; } }
}

.report-body {
  background: white;
  border: 1px solid $border-light;
  border-radius: $radius-md;
  padding: $space-lg;
}
</style>