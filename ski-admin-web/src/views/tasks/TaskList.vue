<template>
  <div class="page-container">
    <!-- 筛选 -->
    <div class="card filter-card">
      <el-form :inline="true" :model="filters" @submit.prevent="handleSearch">
        <el-form-item label="任务类型">
          <el-select v-model="filters.taskType" placeholder="全部" clearable style="width: 140px" @change="handleSearch">
            <el-option label="单次分析" value="single" />
            <el-option label="对比分析" value="comparison" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部" clearable style="width: 140px" @change="handleSearch">
            <el-option label="排队中" value="pending" />
            <el-option label="执行中" value="running" />
            <el-option label="成功" value="success" />
            <el-option label="失败" value="failed" />
          </el-select>
        </el-form-item>
        <el-form-item label="用户ID">
          <el-input
            v-model="filters.userId"
            placeholder="按用户筛选"
            clearable
            style="width: 140px"
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
        <div class="list-title">任务列表</div>
        <div class="list-meta">共 {{ total }} 个任务</div>
      </div>

      <el-table
        v-loading="loading"
        :data="list"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag
              :type="TASK_TYPE[row.taskType]?.type"
              size="small"
              effect="plain"
            >
              {{ TASK_TYPE[row.taskType]?.text || row.taskType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag
              :type="TASK_STATUS[row.status]?.type"
              size="small"
              effect="light"
            >
              <span class="dot" :class="`dot-${TASK_STATUS[row.status]?.type}`"></span>
              {{ TASK_STATUS[row.status]?.text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="userId" label="用户ID" width="100" align="center" />
        <el-table-column label="关联视频" width="180">
          <template #default="{ row }">
            <div v-if="row.videoId" class="link-cell">视频 #{{ row.videoId }}</div>
            <div v-else-if="row.prevVideoId && row.currVideoId" class="link-cell">
              #{{ row.prevVideoId }} ↔ #{{ row.currVideoId }}
            </div>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="LLM 花费" width="110" align="right">
          <template #default="{ row }">
            <span class="num-cell" v-if="row.llmCostYuan != null">{{ formatYuan(row.llmCostYuan) }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="100" align="right">
          <template #default="{ row }">
            <span class="num-cell">{{ getDuration(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.createdTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === 'failed'"
              link
              type="warning"
              @click="handleRetry(row)"
            >重试</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="filters.pageNum"
          v-model:page-size="filters.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
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
      title="任务详情"
      direction="rtl"
      size="560"
    >
      <div v-loading="detailLoading" class="task-detail">
        <div v-if="detail" class="detail-content">
          <div class="detail-section">
            <div class="section-title">基本信息</div>
            <div class="detail-row">
              <span class="label">任务 ID</span>
              <span class="value">#{{ detail.id }}</span>
            </div>
            <div class="detail-row">
              <span class="label">类型</span>
              <span class="value">
                <el-tag size="small" :type="TASK_TYPE[detail.taskType]?.type" effect="plain">
                  {{ TASK_TYPE[detail.taskType]?.text }}
                </el-tag>
              </span>
            </div>
            <div class="detail-row">
              <span class="label">状态</span>
              <span class="value">
                <el-tag size="small" :type="TASK_STATUS[detail.status]?.type">
                  {{ TASK_STATUS[detail.status]?.text }}
                </el-tag>
              </span>
            </div>
            <div class="detail-row">
              <span class="label">用户 ID</span>
              <span class="value">{{ detail.userId }}</span>
            </div>
            <div v-if="detail.videoId" class="detail-row">
              <span class="label">视频 ID</span>
              <span class="value">#{{ detail.videoId }}</span>
            </div>
            <div v-if="detail.prevVideoId" class="detail-row">
              <span class="label">上次视频</span>
              <span class="value">#{{ detail.prevVideoId }}</span>
            </div>
            <div v-if="detail.currVideoId" class="detail-row">
              <span class="label">本次视频</span>
              <span class="value">#{{ detail.currVideoId }}</span>
            </div>
            <div v-if="detail.reportId" class="detail-row">
              <span class="label">报告 ID</span>
              <span class="value">#{{ detail.reportId }}</span>
            </div>
          </div>

          <div class="detail-section">
            <div class="section-title">耗时与费用</div>
            <div class="detail-row">
              <span class="label">创建时间</span>
              <span class="value">{{ formatDateTime(detail.createdTime) }}</span>
            </div>
            <div class="detail-row">
              <span class="label">开始时间</span>
              <span class="value">{{ formatDateTime(detail.startTime) }}</span>
            </div>
            <div class="detail-row">
              <span class="label">完成时间</span>
              <span class="value">{{ formatDateTime(detail.finishTime) }}</span>
            </div>
            <div class="detail-row">
              <span class="label">总耗时</span>
              <span class="value">{{ getDuration(detail) }}</span>
            </div>
            <div class="detail-row">
              <span class="label">输入 tokens</span>
              <span class="value">{{ detail.llmInputTokens || 0 }}</span>
            </div>
            <div class="detail-row">
              <span class="label">输出 tokens</span>
              <span class="value">{{ detail.llmOutputTokens || 0 }}</span>
            </div>
            <div class="detail-row">
              <span class="label">LLM 花费</span>
              <span class="value cost-value">{{ formatYuan(detail.llmCostYuan) }}</span>
            </div>
          </div>

          <div v-if="detail.errorMessage" class="detail-section">
            <div class="section-title danger">失败原因</div>
            <div class="error-box">{{ detail.errorMessage }}</div>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { listTasks, getTaskDetail, retryTask } from '@/api/tasks'
import { TASK_STATUS, TASK_TYPE } from '@/utils/constants'
import { formatDateTime, formatYuan } from '@/utils/format'

const route = useRoute()

const list = ref([])
const total = ref(0)
const loading = ref(false)

const filters = reactive({
  pageNum: 1,
  pageSize: 10,
  taskType: '',
  status: '',
  userId: ''
})

const drawerVisible = ref(false)
const detail = ref(null)
const detailLoading = ref(false)

onMounted(() => {
  // 支持从 Dashboard 跳转带 status 过滤
  if (route.query.status) {
    filters.status = String(route.query.status)
  }
  loadList()
})

async function loadList() {
  loading.value = true
  try {
    const params = { ...filters }
    Object.keys(params).forEach(k => {
      if (params[k] === '' || params[k] === null) delete params[k]
    })
    const data = await listTasks(params)
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
  filters.taskType = ''
  filters.status = ''
  filters.userId = ''
  loadList()
}

async function openDetail(row) {
  drawerVisible.value = true
  detailLoading.value = true
  try {
    detail.value = await getTaskDetail(row.id)
  } finally {
    detailLoading.value = false
  }
}

async function handleRetry(row) {
  try {
    await ElMessageBox.confirm(
      `确定重新执行任务 #${row.id} 吗?`,
      '确认重试',
      { confirmButtonText: '重试', cancelButtonText: '取消', type: 'warning' }
    )
  } catch { return }

  await retryTask(row.id)
  ElMessage.success('已重新加入队列')
  loadList()
}

function getDuration(task) {
  if (!task.startTime) return '-'
  const start = dayjs(task.startTime)
  const end = task.finishTime ? dayjs(task.finishTime) : dayjs()
  const seconds = end.diff(start, 'second')
  if (seconds < 60) return `${seconds}s`
  const minutes = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${minutes}m${s}s`
}
</script>

<style lang="scss" scoped>
.filter-card {
  margin-bottom: $space-md;
  padding: $space-md $space-lg;

  :deep(.el-form--inline .el-form-item) {
    margin-bottom: 0;
  }
}

.list-card { padding: 0; }

.list-header {
  padding: $space-md $space-lg;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid $border-light;

  .list-title {
    font-size: $font-md;
    font-weight: 600;
  }
  .list-meta {
    font-size: $font-sm;
    color: $text-secondary;
  }
}

.num-cell {
  font-variant-numeric: tabular-nums;
  font-weight: 500;
}

.link-cell {
  color: $color-primary;
  font-variant-numeric: tabular-nums;
}

.text-muted { color: $text-placeholder; }

.pagination {
  padding: $space-md $space-lg;
  display: flex;
  justify-content: flex-end;
}

// 抽屉
.task-detail { padding: $space-md; }

.detail-section { margin-bottom: $space-lg; }

.section-title {
  font-size: $font-md;
  font-weight: 600;
  margin-bottom: $space-md;
  padding-bottom: $space-sm;
  border-bottom: 2px solid $color-primary;
  display: inline-block;
  padding-right: $space-md;

  &.danger {
    border-bottom-color: $color-danger;
    color: $color-danger;
  }
}

.detail-row {
  display: flex;
  padding: $space-sm 0;
  font-size: $font-sm;
  border-bottom: 1px solid $border-light;

  &:last-child { border-bottom: none; }

  .label {
    width: 100px;
    color: $text-secondary;
  }
  .value {
    flex: 1;
    color: $text-primary;

    &.cost-value {
      color: $color-warning;
      font-weight: 600;
    }
  }
}

.error-box {
  padding: $space-md;
  background: rgba(239, 68, 68, 0.05);
  border: 1px solid rgba(239, 68, 68, 0.2);
  border-radius: $radius-md;
  color: $color-danger;
  font-size: $font-sm;
  word-break: break-all;
  font-family: "SF Mono", Consolas, monospace;
}
</style>
