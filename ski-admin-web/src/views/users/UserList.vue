<template>
  <div class="page-container">
    <!-- 筛选 -->
    <div class="card filter-card">
      <el-form :inline="true" :model="filters" @submit.prevent="handleSearch">
        <el-form-item label="手机号">
          <el-input
            v-model="filters.phone"
            placeholder="模糊搜索手机号"
            clearable
            style="width: 220px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部" clearable style="width: 140px" @change="handleSearch">
            <el-option label="正常" :value="1" />
            <el-option label="封禁" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 列表 -->
    <div class="card list-card">
      <div class="list-header">
        <div class="list-title">用户列表</div>
        <div class="list-meta">共 {{ total }} 个用户</div>
      </div>

      <el-table
        v-loading="loading"
        :data="list"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="phone" label="手机号" min-width="140" />
        <el-table-column prop="nickname" label="昵称" min-width="140">
          <template #default="{ row }">
            {{ row.nickname || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="视频数" width="100" align="center">
          <template #default="{ row }">
            <span class="num-cell">{{ row.videoCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="报告数" width="100" align="center">
          <template #default="{ row }">
            <span class="num-cell">{{ row.reportCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag
              :type="USER_STATUS[row.status]?.type"
              size="small"
              effect="light"
            >
              {{ USER_STATUS[row.status]?.text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === 1"
              link
              type="danger"
              @click="handleToggleStatus(row, 0)"
            >封禁</el-button>
            <el-button
              v-else
              link
              type="success"
              @click="handleToggleStatus(row, 1)"
            >解封</el-button>
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
      title="用户详情"
      direction="rtl"
      size="500"
    >
      <div v-loading="detailLoading" class="user-detail">
        <div v-if="detail" class="detail-content">
          <div class="detail-section">
            <div class="section-title">基本信息</div>
            <div class="detail-row">
              <span class="label">用户 ID</span>
              <span class="value">{{ detail.id }}</span>
            </div>
            <div class="detail-row">
              <span class="label">手机号</span>
              <span class="value">{{ detail.phone }}</span>
            </div>
            <div class="detail-row">
              <span class="label">昵称</span>
              <span class="value">{{ detail.nickname || '-' }}</span>
            </div>
            <div class="detail-row">
              <span class="label">状态</span>
              <span class="value">
                <el-tag :type="USER_STATUS[detail.status]?.type" size="small">
                  {{ USER_STATUS[detail.status]?.text }}
                </el-tag>
              </span>
            </div>
            <div class="detail-row">
              <span class="label">注册时间</span>
              <span class="value">{{ formatDateTime(detail.createdTime) }}</span>
            </div>
            <div class="detail-row">
              <span class="label">更新时间</span>
              <span class="value">{{ formatDateTime(detail.updateTime) }}</span>
            </div>
          </div>

          <div class="detail-section">
            <div class="section-title">使用数据</div>
            <div class="stats-row">
              <div class="stat-block">
                <div class="stat-num">{{ detail.videoCount || 0 }}</div>
                <div class="stat-lbl">视频数</div>
              </div>
              <div class="stat-block">
                <div class="stat-num">{{ detail.reportCount || 0 }}</div>
                <div class="stat-lbl">报告数</div>
              </div>
            </div>
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
import { Search } from '@element-plus/icons-vue'
import { listUsers, getUserDetail, updateUserStatus } from '@/api/users'
import { USER_STATUS } from '@/utils/constants'
import { formatDateTime } from '@/utils/format'

const route = useRoute()

const list = ref([])
const total = ref(0)
const loading = ref(false)

const filters = reactive({
  pageNum: 1,
  pageSize: 10,
  phone: '',
  status: null
})

const drawerVisible = ref(false)
const detail = ref(null)
const detailLoading = ref(false)

onMounted(() => loadList())

async function loadList() {
  loading.value = true
  try {
    const params = { ...filters }
    if (params.status === null || params.status === '') delete params.status
    if (!params.phone) delete params.phone
    const data = await listUsers(params)
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
  filters.phone = ''
  filters.status = null
  loadList()
}

async function openDetail(row) {
  drawerVisible.value = true
  detailLoading.value = true
  try {
    detail.value = await getUserDetail(row.id)
  } finally {
    detailLoading.value = false
  }
}

async function handleToggleStatus(row, newStatus) {
  const action = newStatus === 0 ? '封禁' : '解封'
  try {
    await ElMessageBox.confirm(
      `确定${action}用户 "${row.phone}" 吗?`,
      '确认',
      {
        confirmButtonText: action,
        cancelButtonText: '取消',
        type: newStatus === 0 ? 'warning' : 'info'
      }
    )
  } catch { return }

  await updateUserStatus(row.id, newStatus)
  ElMessage.success(`已${action}`)
  loadList()
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

.list-card {
  padding: 0;
}

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

.pagination {
  padding: $space-md $space-lg;
  display: flex;
  justify-content: flex-end;
}

// 抽屉
.user-detail {
  padding: $space-md;
}

.detail-section {
  margin-bottom: $space-lg;
}

.section-title {
  font-size: $font-md;
  font-weight: 600;
  margin-bottom: $space-md;
  padding-bottom: $space-sm;
  border-bottom: 2px solid $color-primary;
  display: inline-block;
  padding-right: $space-md;
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
  }
}

.stats-row {
  display: flex;
  gap: $space-md;
}

.stat-block {
  flex: 1;
  text-align: center;
  padding: $space-md;
  background: $bg-page;
  border-radius: $radius-md;

  .stat-num {
    font-size: 28px;
    font-weight: 700;
    color: $color-primary;
  }
  .stat-lbl {
    font-size: $font-xs;
    color: $text-secondary;
    margin-top: 4px;
  }
}
</style>
