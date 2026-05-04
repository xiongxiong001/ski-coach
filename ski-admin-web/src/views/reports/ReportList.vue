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
          <div class="list-title">单次报告</div>
          <div class="list-sub">所有用户的 AI 教练报告 · 用于审阅 AI 输出质量</div>
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
        <el-table-column label="视频" min-width="180">
          <template #default="{ row }">
            <div class="video-cell" :title="row.videoFilename">
              <el-icon><VideoCamera /></el-icon>
              <span class="video-name">{{ row.videoFilename || `#${row.videoId}` }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="报告摘要" min-width="280" show-overflow-tooltip>
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
      title="教练报告详情"
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

          <!-- 视频信息 -->
          <div class="meta-card">
            <div class="card-section-title">关联视频</div>
            <div class="meta-row">
              <div class="meta-item">
                <div class="meta-label">文件名</div>
                <div class="meta-value mono">{{ detail.videoFilename || '(已删除)' }}</div>
              </div>
              <div class="meta-item">
                <div class="meta-label">检测率</div>
                <div class="meta-value strong">{{ formatPercent(detail.videoDetectionRate) }}</div>
              </div>
              <div class="meta-item">
                <div class="meta-label">时长</div>
                <div class="meta-value">{{ detail.videoDurationSeconds ? `${Number(detail.videoDurationSeconds).toFixed(0)}秒` : '-' }}</div>
              </div>
            </div>
            <div class="meta-row">
              <div class="meta-item">
                <div class="meta-label">左转</div>
                <div class="meta-value">{{ detail.turnLeftCount ?? '-' }}</div>
              </div>
              <div class="meta-item">
                <div class="meta-label">右转</div>
                <div class="meta-value">{{ detail.turnRightCount ?? '-' }}</div>
              </div>
              <div class="meta-item">
                <div class="meta-label">视频ID</div>
                <div class="meta-value">#{{ detail.videoId }}</div>
              </div>
            </div>
          </div>

          <!-- 成本信息 -->
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
            <div class="card-section-title">教练报告全文</div>
            <MarkdownView :content="detail.reportMarkdown" />
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh, VideoCamera } from '@element-plus/icons-vue'
import { listReports, getReportDetail } from '@/api/reports'
import { formatDateTime, formatYuan, formatNumber, formatPercent } from '@/utils/format'
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
    const data = await listReports(params)
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
    detail.value = await getReportDetail(row.id)
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
  .user-name {
    font-size: $font-sm;
    color: $text-primary;
    font-weight: 500;
  }
  .user-phone {
    font-size: $font-xs;
    color: $text-secondary;
    font-variant-numeric: tabular-nums;
  }
}

.video-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  color: $text-regular;
  font-size: $font-sm;

  .video-name {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 160px;
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

// 详情抽屉
.report-detail {
  padding: $space-md;
}

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

    &.mono {
      font-family: "SF Mono", Consolas, monospace;
      font-size: $font-xs;
    }
    &.time-value {
      font-variant-numeric: tabular-nums;
      font-size: $font-xs;
    }
    &.strong {
      color: $color-primary;
    }
    &.cost-value {
      font-size: $font-md;
      color: $color-warning;
      font-weight: 700;
    }
  }
  .meta-sub {
    font-size: $font-xs;
    color: $text-secondary;
    margin-top: 2px;
    font-variant-numeric: tabular-nums;
  }
}

.report-body {
  background: white;
  border: 1px solid $border-light;
  border-radius: $radius-md;
  padding: $space-lg;
}
</style>
