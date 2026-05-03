<template>
  <div class="page-container">
    <div class="back-bar">
      <el-button link @click="router.back()">
        <el-icon><ArrowLeft /></el-icon>&nbsp;返回
      </el-button>
    </div>

    <div v-loading="loading" class="card">
      <div v-if="video" class="video-header">
        <div class="title-row">
          <h2>{{ video.originalFilename }}</h2>
          <el-tag :type="VIDEO_STATUS[video.analysisStatus]?.type">
            {{ VIDEO_STATUS[video.analysisStatus]?.text }}
          </el-tag>
        </div>

        <div class="meta-grid">
          <div class="meta-item">
            <div class="meta-label">文件大小</div>
            <div class="meta-value">{{ formatFileSize(video.fileSize) }}</div>
          </div>
          <div class="meta-item">
            <div class="meta-label">视频时长</div>
            <div class="meta-value">{{ formatDuration(video.durationSeconds) }}</div>
          </div>
          <div class="meta-item">
            <div class="meta-label">分辨率</div>
            <div class="meta-value">
              <span v-if="video.width && video.height">{{ video.width }}×{{ video.height }}</span>
              <span v-else>-</span>
            </div>
          </div>
          <div class="meta-item">
            <div class="meta-label">上传时间</div>
            <div class="meta-value">{{ formatDateTime(video.createdTime) }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 分析中:显示进度组件(轮询) -->
    <div
      v-if="video && (video.analysisStatus === 'pending' || video.analysisStatus === 'analyzing')"
      class="card mt-lg"
    >
      <h3 class="mb-md">AI 分析进度</h3>
      <div class="analyzing-tip">
        <el-icon class="loading-icon" :size="24" color="#2563eb"><Loading /></el-icon>
        <div>
          <div>视频正在 AI 分析中,通常需要 1-3 分钟</div>
          <div class="text-secondary mt-md">页面会自动刷新,你也可以先去做别的事</div>
        </div>
      </div>
    </div>

    <!-- 分析完成:显示统计 + 跳报告按钮 -->
    <div v-if="video && video.analysisStatus === 'analyzed'" class="card mt-lg">
      <h3 class="mb-md">分析结果概览</h3>
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-num">{{ formatPercent(video.detectionRate) }}</div>
          <div class="stat-label">姿态检测率</div>
        </div>
        <div class="stat-card">
          <div class="stat-num">{{ video.turnLeftCount || 0 }}</div>
          <div class="stat-label">左转次数</div>
        </div>
        <div class="stat-card">
          <div class="stat-num">{{ video.turnRightCount || 0 }}</div>
          <div class="stat-label">右转次数</div>
        </div>
      </div>

      <div class="action-bar">
        <el-button type="primary" size="large" @click="goReport">
          📄 查看完整教练报告
        </el-button>
        <el-button @click="router.push('/comparison/create')">📊 与其他视频对比</el-button>
      </div>
    </div>

    <!-- 分析失败 -->
    <div v-if="video && video.analysisStatus === 'failed'" class="card mt-lg failed-card">
      <h3 class="mb-md" style="color: #ef4444">😢 分析失败</h3>
      <div class="error-message">
        {{ video.analysisErrorMessage || '未知错误' }}
      </div>
      <div class="text-secondary mt-md">
        如多次失败,请联系管理员处理。也可以重新上传一个清晰、单人、第三人称视角的视频试试。
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getVideo } from '@/api/video'
import { listReports } from '@/api/report'
import { VIDEO_STATUS, TASK_POLL_INTERVAL } from '@/utils/constants'
import { formatFileSize, formatDateTime, formatDuration, formatPercent } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const video = ref(null)
const loading = ref(false)
const pollTimer = ref(null)

onMounted(async () => {
  await loadVideo()
  // 如果还在分析中,启动轮询
  if (video.value && (video.value.analysisStatus === 'pending' || video.value.analysisStatus === 'analyzing')) {
    pollTimer.value = setInterval(loadVideo, TASK_POLL_INTERVAL)
  }
})

onUnmounted(() => {
  if (pollTimer.value) clearInterval(pollTimer.value)
})

async function loadVideo() {
  loading.value = !video.value   // 首次加载显示loading,后续轮询不显示
  try {
    const data = await getVideo(route.params.id)
    video.value = data
    // 状态变化后停止轮询
    if (data.analysisStatus === 'analyzed' || data.analysisStatus === 'failed') {
      if (pollTimer.value) {
        clearInterval(pollTimer.value)
        pollTimer.value = null
      }
    }
  } finally {
    loading.value = false
  }
}

async function goReport() {
  // 我们没有"video → report"的直接接口,通过列表筛 video_id
  // 拿到当前用户最新的报告中,video_id 匹配的那个
  // 简化:直接拉报告列表的第一页找
  try {
    const data = await listReports({ pageNum: 1, pageSize: 50 })
    const report = (data.records || []).find(r => r.videoId === video.value.id)
    if (report) {
      router.push(`/reports/${report.id}`)
    } else {
      ElMessage.warning('暂未找到对应报告,请稍后再试')
    }
  } catch {
    ElMessage.error('打开报告失败')
  }
}
</script>

<style lang="scss" scoped>
.back-bar { margin-bottom: $space-md; }

.video-header {
  .title-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: $space-lg;
    gap: $space-md;

    h2 {
      font-size: 20px;
      font-weight: 600;
      flex: 1;
      word-break: break-all;
    }
  }

  .meta-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: $space-md;

    @media (max-width: 768px) {
      grid-template-columns: repeat(2, 1fr);
    }
  }

  .meta-item {
    .meta-label { font-size: 13px; color: $text-secondary; margin-bottom: 2px; }
    .meta-value { font-size: 15px; font-weight: 500; color: $text-primary; }
  }
}

.analyzing-tip {
  display: flex;
  align-items: center;
  gap: $space-md;
  padding: $space-lg;
  background: #eff6ff;
  border-radius: $radius-md;
}
.loading-icon { animation: rotate 1.5s linear infinite; }
@keyframes rotate { 100% { transform: rotate(360deg); } }

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: $space-md;
  margin-bottom: $space-lg;
}

.stat-card {
  text-align: center;
  padding: $space-lg;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  border-radius: $radius-lg;

  .stat-num { font-size: 32px; font-weight: 600; color: $color-primary; }
  .stat-label { font-size: 14px; color: $text-secondary; margin-top: $space-xs; }
}

.action-bar {
  display: flex;
  gap: $space-md;
  flex-wrap: wrap;
}

.failed-card {
  border-color: #fca5a5;

  .error-message {
    padding: $space-md;
    background: #fef2f2;
    border-radius: $radius-md;
    color: $color-danger;
    word-break: break-all;
  }
}
</style>
