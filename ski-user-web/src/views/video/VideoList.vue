<template>
  <div class="page-container">
    <div class="page-header">
      <h2>我的视频</h2>
      <el-upload
        :before-upload="handleBeforeUpload"
        :http-request="handleUpload"
        :show-file-list="false"
        accept=".mp4,.mov,.m4v"
      >
        <el-button type="primary" :loading="uploading">
          <el-icon><Upload /></el-icon>&nbsp;上传视频
        </el-button>
      </el-upload>
    </div>

    <!-- 上传进度条 -->
    <div v-if="uploading || uploadingTaskId" class="card mb-lg">
      <div v-if="uploading">
        <div class="flex-between mb-md">
          <span><strong>{{ uploadingFile?.name }}</strong> 上传中</span>
          <span class="text-secondary">{{ uploadProgress }}%</span>
        </div>
        <el-progress :percentage="uploadProgress" :stroke-width="8" />
      </div>

      <TaskProgress
        v-if="uploadingTaskId"
        :task-id="uploadingTaskId"
        @success="onTaskSuccess"
        @failed="onTaskFailed"
      />
    </div>

    <!-- 状态筛选 -->
    <div class="filter-bar mb-md">
      <el-radio-group v-model="filterStatus" @change="loadList">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="pending">待分析</el-radio-button>
        <el-radio-button value="analyzing">分析中</el-radio-button>
        <el-radio-button value="analyzed">已完成</el-radio-button>
        <el-radio-button value="failed">失败</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 列表 -->
    <div v-loading="loading">
      <EmptyState
        v-if="!loading && !list.length"
        icon="🎬"
        title="还没有视频"
        description="上传你的第一个滑雪视频,获得 AI 教练点评"
      />

      <div v-else class="video-grid">
        <div
          v-for="v in list"
          :key="v.id"
          class="video-item card"
          @click="goDetail(v)"
        >
          <div class="video-icon">🎬</div>
          <div class="video-info">
            <div class="video-name">{{ v.originalFilename }}</div>
            <div class="video-meta">
              <span>{{ formatFileSize(v.fileSize) }}</span>
              <span v-if="v.durationSeconds">·  {{ formatDuration(v.durationSeconds) }}</span>
              <span>·  {{ formatDateTime(v.createdTime) }}</span>
            </div>
            <div v-if="v.analysisStatus === 'analyzed'" class="video-stats">
              <span>检测率: <strong>{{ formatPercent(v.detectionRate) }}</strong></span>
              <span>·  左转: <strong>{{ v.turnLeftCount || 0 }}</strong></span>
              <span>·  右转: <strong>{{ v.turnRightCount || 0 }}</strong></span>
            </div>
          </div>
          <div class="video-actions">
            <el-tag :type="VIDEO_STATUS[v.analysisStatus]?.type || 'info'" size="small">
              {{ VIDEO_STATUS[v.analysisStatus]?.text || v.analysisStatus }}
            </el-tag>
            <el-button
              link
              type="danger"
              size="small"
              @click.stop="handleDelete(v)"
            >删除</el-button>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="total > pageSize" class="pagination">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          background
          layout="prev, pager, next, total"
          @current-change="loadList"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Upload } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listVideos, uploadVideo, deleteVideo } from '@/api/video'
import { VIDEO_STATUS, MAX_FILE_SIZE_MB, ALLOWED_VIDEO_EXTENSIONS } from '@/utils/constants'
import { formatFileSize, formatDateTime, formatDuration, formatPercent } from '@/utils/format'
import TaskProgress from '@/components/TaskProgress.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()

const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const filterStatus = ref('')
const loading = ref(false)

const uploading = ref(false)
const uploadingFile = ref(null)
const uploadProgress = ref(0)
const uploadingTaskId = ref(null)

onMounted(() => loadList())

async function loadList() {
  loading.value = true
  try {
    const data = await listVideos({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      analysisStatus: filterStatus.value || undefined
    })
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function handleBeforeUpload(file) {
  // 校验扩展名
  const ext = file.name.split('.').pop()?.toLowerCase()
  if (!ALLOWED_VIDEO_EXTENSIONS.includes(ext)) {
    ElMessage.error(`只支持以下格式: ${ALLOWED_VIDEO_EXTENSIONS.join(', ')}`)
    return false
  }
  // 校验大小
  const sizeMB = file.size / 1024 / 1024
  if (sizeMB > MAX_FILE_SIZE_MB) {
    ElMessage.error(`文件最大 ${MAX_FILE_SIZE_MB}MB`)
    return false
  }
  return true
}

async function handleUpload({ file }) {
  uploading.value = true
  uploadingFile.value = file
  uploadProgress.value = 0
  uploadingTaskId.value = null

  try {
    const data = await uploadVideo(file, (p) => {
      uploadProgress.value = p
    })
    uploading.value = false

    if (data.instantUpload) {
      ElMessage.success('秒传成功(您已上传过相同文件)')
      // 秒传:直接跳详情
      router.push(`/videos/${data.videoId}`)
      return
    }

    ElMessage.success('上传成功,AI 正在分析视频...')

    // 刷新列表显示新视频
    loadList()

    // 找到刚上传视频的 task_id 进行轮询
    // 简化处理:重新请求视频详情接口的 N 次轮询拿到 task,这里我们暂时直接刷新列表
    // P3.2 阶段会从上传响应里直接拿到 task_id(后端目前未返回,需要改一下)
    // 临时方案:延迟2秒后跳到该视频详情页,在详情页显示进度
    setTimeout(() => {
      router.push(`/videos/${data.videoId}`)
    }, 1500)
  } catch (e) {
    uploading.value = false
  }
}

function onTaskSuccess(task) {
  ElMessage.success('AI 分析完成!')
  uploadingTaskId.value = null
  if (task.reportId) {
    router.push(`/reports/${task.reportId}`)
  } else {
    loadList()
  }
}

function onTaskFailed() {
  uploadingTaskId.value = null
  loadList()
}

function goDetail(v) {
  router.push(`/videos/${v.id}`)
}

async function handleDelete(v) {
  try {
    await ElMessageBox.confirm(`确定删除视频 "${v.originalFilename}" 吗?`, '提示', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  await deleteVideo(v.id)
  ElMessage.success('已删除')
  loadList()
}
</script>

<style lang="scss" scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $space-lg;

  h2 { font-size: 22px; font-weight: 600; }
}

.filter-bar {
  display: flex;
  align-items: center;
}

.video-grid {
  display: flex;
  flex-direction: column;
  gap: $space-md;
}

.video-item {
  display: flex;
  align-items: center;
  gap: $space-md;
  cursor: pointer;
  transition: all .15s;
  padding: $space-md $space-lg;

  &:hover {
    box-shadow: $shadow-md;
    border-color: $color-primary-light;
  }

  .video-icon {
    width: 56px;
    height: 56px;
    background: linear-gradient(135deg, $color-primary-light, $color-primary);
    border-radius: $radius-md;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 28px;
    flex-shrink: 0;
  }

  .video-info {
    flex: 1;
    min-width: 0;

    .video-name {
      font-size: 15px;
      font-weight: 500;
      color: $text-primary;
      margin-bottom: 4px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .video-meta {
      font-size: 13px;
      color: $text-secondary;
      span { margin-right: 4px; }
    }
    .video-stats {
      margin-top: $space-xs;
      font-size: 13px;
      color: $text-regular;

      strong { color: $color-primary-dark; margin-right: 2px; }
    }
  }

  .video-actions {
    display: flex;
    flex-direction: column;
    gap: $space-sm;
    align-items: flex-end;
  }
}

.pagination {
  margin-top: $space-lg;
  display: flex;
  justify-content: center;
}
</style>
