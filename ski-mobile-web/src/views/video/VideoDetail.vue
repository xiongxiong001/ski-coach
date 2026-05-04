<template>
  <div class="video-detail-page">
    <!-- 顶栏 -->
    <header class="nav-bar">
      <button class="back-btn" @click="router.back()">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
          <path d="M15 19L8 12l7-7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
      </button>
      <h1 class="nav-title">视频详情</h1>
      <button class="more-btn" @click="handleDelete">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
          <path d="M3 6h18M8 6V4a2 2 0 012-2h4a2 2 0 012 2v2m3 0v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6h14z"
                stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
      </button>
    </header>

    <div v-if="loading && !video" class="loading-skeleton">
      <div class="skeleton" style="height: 220px; margin-bottom: 16px;"></div>
      <div class="skeleton" style="height: 120px; margin-bottom: 16px;"></div>
      <div class="skeleton" style="height: 200px;"></div>
    </div>

    <div v-else-if="video" class="content">
      <!-- 视频播放器 -->
      <div class="video-player-wrap">
        <video
          ref="videoRef"
          class="video-player"
          :src="`/api/videos/${video.id}/stream?token=${userStore.token}`"
          controls
          preload="metadata"
          playsinline
          webkit-playsinline
        >
          您的浏览器不支持视频播放
        </video>
        <div class="video-overlay">
          <div class="status-badge" :class="`status-${video.analysisStatus}`">
            <span class="status-dot"></span>
            {{ VIDEO_STATUS[video.analysisStatus]?.text }}
          </div>
        </div>
      </div>

      <!-- 视频信息 -->
      <div class="info-card">
        <div class="info-name">{{ video.originalFilename }}</div>
        <div class="info-grid">
          <div class="info-item">
            <div class="info-label">大小</div>
            <div class="info-value">{{ formatFileSize(video.fileSize) }}</div>
          </div>
          <div class="info-item">
            <div class="info-label">时长</div>
            <div class="info-value">{{ formatDuration(video.durationSeconds) }}</div>
          </div>
          <div class="info-item">
            <div class="info-label">分辨率</div>
            <div class="info-value">{{ video.width && video.height ? `${video.width}×${video.height}` : '-' }}</div>
          </div>
          <div class="info-item">
            <div class="info-label">上传</div>
            <div class="info-value">{{ formatDate(video.createdTime) }}</div>
          </div>
        </div>
      </div>

      <!-- 分析中(分析进度) -->
      <div v-if="video.analysisStatus === 'pending' || video.analysisStatus === 'analyzing'"
           class="analyzing-card">
        <div class="analyzing-icon">
          <div class="ai-pulse"></div>
          <div class="ai-pulse delay-1"></div>
          <div class="ai-pulse delay-2"></div>
          <div class="ai-icon">🤖</div>
        </div>
        <div class="analyzing-title">{{ analyzingText }}</div>
        <div class="analyzing-subtitle">通常需要 1-3 分钟,完成后会自动刷新</div>
        <div class="dots">
          <div class="dot"></div>
          <div class="dot"></div>
          <div class="dot"></div>
        </div>
      </div>

      <!-- 已完成:数据指标卡片 -->
      <div v-else-if="video.analysisStatus === 'analyzed'" class="metrics-section">
        <h3 class="section-title">分析结果</h3>

        <!-- 大数字面板 -->
        <div class="big-stats">
          <div class="big-stat detection-stat">
            <div class="stat-num">{{ formatPercent(video.detectionRate) }}</div>
            <div class="stat-label">姿态检测率</div>
            <!-- 进度条 -->
            <div class="stat-bar">
              <div class="stat-bar-fill" :style="{ width: detectionPercent + '%' }"></div>
            </div>
          </div>
        </div>

        <!-- 转弯统计 -->
        <div class="turn-stats">
          <div class="turn-card turn-left">
            <div class="turn-icon">↺</div>
            <div class="turn-num">{{ video.turnLeftCount || 0 }}</div>
            <div class="turn-label">左转次数</div>
          </div>
          <div class="turn-card turn-right">
            <div class="turn-icon">↻</div>
            <div class="turn-num">{{ video.turnRightCount || 0 }}</div>
            <div class="turn-label">右转次数</div>
          </div>
        </div>

        <!-- CTA 按钮 -->
        <div class="actions">
          <button class="primary-btn" @click="goReport">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
              <path d="M9 12h6M9 16h6M9 8h6M5 21V5a2 2 0 012-2h10a2 2 0 012 2v16l-3.5-2.5L12 21l-3.5-2.5L5 21z"
                    stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
            查看完整教练报告
          </button>
          <button class="secondary-btn" @click="goToCompare">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
              <path d="M3 4h6v16H3zM15 4h6v8h-6zM15 16h6v4h-6z"
                    stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
            </svg>
            与其他视频对比
          </button>
        </div>
      </div>

      <!-- 失败 -->
      <div v-else-if="video.analysisStatus === 'failed'" class="failed-card">
        <div class="failed-icon">😢</div>
        <div class="failed-title">分析失败</div>
        <div class="failed-message">{{ video.analysisErrorMessage || '未知错误' }}</div>
        <div class="failed-tip">建议:第三人称视角、单人画面、光线充足重新拍摄</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showSuccessToast, showFailToast, showConfirmDialog } from 'vant'
import { useUserStore } from '@/stores/user'
import { getVideo, deleteVideo } from '@/api/video'
import { listReports } from '@/api/report'
import { VIDEO_STATUS, TASK_POLL_INTERVAL } from '@/utils/constants'
import { formatFileSize, formatDate, formatDuration, formatPercent } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const video = ref(null)
const videoRef = ref(null)
const loading = ref(false)
const pollTimer = ref(null)
const dotIndex = ref(0)

const detectionPercent = computed(() => {
  if (!video.value?.detectionRate) return 0
  return Math.round(video.value.detectionRate * 100)
})

const analyzingText = computed(() => {
  if (video.value?.analysisStatus === 'pending') return 'AI 排队中,马上开始'
  return 'AI 教练正在分析中'
})

onMounted(async () => {
  await loadVideo()
  // 如果还在分析中,启动轮询
  if (video.value && (video.value.analysisStatus === 'pending' || video.value.analysisStatus === 'analyzing')) {
    pollTimer.value = setInterval(loadVideo, TASK_POLL_INTERVAL)
    // 同时让 dots 动起来
    setInterval(() => { dotIndex.value = (dotIndex.value + 1) % 3 }, 600)
  }
})

onUnmounted(() => {
  if (pollTimer.value) clearInterval(pollTimer.value)
})

async function loadVideo() {
  loading.value = !video.value
  try {
    const data = await getVideo(route.params.id)
    video.value = data
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
  try {
    const data = await listReports({ pageNum: 1, pageSize: 50 })
    const report = (data.records || []).find(r => r.videoId === video.value.id)
    if (report) {
      router.push(`/reports/${report.id}`)
    } else {
      showFailToast('暂未找到对应报告,请稍后再试')
    }
  } catch {
    showFailToast('打开报告失败')
  }
}

function goToCompare() {
  // 把当前视频带过去作为"本次"
  router.push({
    path: '/comparison/create',
    query: { from: video.value.id }
  })
}

async function handleDelete() {
  try {
    await showConfirmDialog({
      title: '确认删除',
      message: '删除后该视频不可恢复,但已生成的报告会保留',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      confirmButtonColor: '#EF4444'
    })
  } catch {
    return
  }
  await deleteVideo(video.value.id)
  showSuccessToast('已删除')
  router.back()
}
</script>

<style lang="scss" scoped>
.video-detail-page {
  min-height: 100vh;
  background: $bg-base;
  padding-bottom: $space-2xl;
}

// ====== 顶栏 ======
.nav-bar {
  position: sticky;
  top: 0;
  z-index: $z-navbar;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px $space-md;
  padding-top: calc(12px + #{$safe-top});
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid $border-light;

  .back-btn, .more-btn {
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: $text-primary;
    cursor: pointer;
  }

  .nav-title {
    font-size: $font-md;
    font-weight: 600;
    color: $text-primary;
  }
}

.content {
  padding: $space-md $space-lg;
}

// ====== 视频播放器 ======
.video-player-wrap {
  position: relative;
  margin-bottom: $space-lg;
  border-radius: $radius-xl;
  overflow: hidden;
  background: #000;
}

.video-player {
  display: block;
  width: 100%;
  max-height: 50vh;
  object-fit: contain;
  outline: none;
}

.video-overlay {
  position: absolute;
  top: $space-md;
  left: $space-md;
}

.status-badge {
  padding: 6px 12px;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(10px);
  border-radius: $radius-full;
  font-size: $font-xs;
  color: white;
  display: flex;
  align-items: center;
  gap: 6px;

  .status-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: $color-info;
  }
  &.status-analyzed .status-dot { background: $color-success; box-shadow: 0 0 8px $color-success; }
  &.status-analyzing .status-dot { background: $color-warning; animation: pulse 1.4s infinite; }
  &.status-pending .status-dot { background: $color-info; animation: pulse 1.4s infinite; }
  &.status-failed .status-dot { background: $color-danger; }
}

// ====== 视频信息卡 ======
.info-card {
  background: $bg-card;
  border: 1px solid $border-light;
  border-radius: $radius-lg;
  padding: $space-lg;
  margin-bottom: $space-lg;
}

.info-name {
  font-size: $font-md;
  font-weight: 500;
  color: $text-primary;
  margin-bottom: $space-md;
  word-break: break-all;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $space-md;
}

.info-item {
  .info-label {
    font-size: $font-xs;
    color: $text-secondary;
    margin-bottom: 2px;
  }
  .info-value {
    font-size: $font-sm;
    color: $text-primary;
    font-weight: 500;
  }
}

// ====== 分析中卡片 ======
.analyzing-card {
  background: $bg-card;
  border: 1px solid $border-light;
  border-radius: $radius-lg;
  padding: 40px $space-lg;
  text-align: center;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: -50%; left: -50%;
    width: 200%; height: 200%;
    background: radial-gradient(circle, rgba(59,130,246,0.08) 0%, transparent 50%);
    animation: rotate-bg 8s linear infinite;
  }
}

@keyframes rotate-bg {
  to { transform: rotate(360deg); }
}

.analyzing-icon {
  position: relative;
  width: 80px;
  height: 80px;
  margin: 0 auto $space-lg;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1;

  .ai-pulse {
    position: absolute;
    inset: 0;
    border: 2px solid $color-primary;
    border-radius: 50%;
    animation: ai-pulse 2s infinite;
    opacity: 0;
  }
  .delay-1 { animation-delay: 0.6s; }
  .delay-2 { animation-delay: 1.2s; }

  .ai-icon {
    font-size: 40px;
    z-index: 2;
  }
}

@keyframes ai-pulse {
  0% { transform: scale(0.6); opacity: 0.8; }
  100% { transform: scale(1.6); opacity: 0; }
}

.analyzing-title {
  font-size: $font-lg;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 4px;
  position: relative;
  z-index: 1;
}

.analyzing-subtitle {
  font-size: $font-sm;
  color: $text-secondary;
  position: relative;
  z-index: 1;
}

.dots {
  display: flex;
  justify-content: center;
  gap: 6px;
  margin-top: $space-md;
  position: relative;
  z-index: 1;

  .dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: $color-primary;
    animation: dots-jump 1.4s infinite ease-in-out;

    &:nth-child(1) { animation-delay: 0s; }
    &:nth-child(2) { animation-delay: 0.2s; }
    &:nth-child(3) { animation-delay: 0.4s; }
  }
}

@keyframes dots-jump {
  0%, 80%, 100% { opacity: 0.3; transform: scale(0.8); }
  40% { opacity: 1; transform: scale(1.2); }
}

// ====== 数据指标 ======
.metrics-section {
  animation: fadeIn 0.4s ease-out;
}

.section-title {
  font-size: $font-xl;
  font-weight: 600;
  margin-bottom: $space-md;
  color: $text-primary;
}

.big-stats {
  margin-bottom: $space-md;
}

.big-stat {
  background: $gradient-cyan;
  border-radius: $radius-lg;
  padding: $space-xl;
  position: relative;
  overflow: hidden;
  color: white;

  &::before {
    content: '';
    position: absolute;
    top: -50%; right: -20%;
    width: 200px; height: 200px;
    background: radial-gradient(circle, rgba(255,255,255,0.2) 0%, transparent 60%);
    border-radius: 50%;
  }

  .stat-num {
    font-size: 48px;
    font-weight: 700;
    line-height: 1;
    margin-bottom: 4px;
    letter-spacing: -1px;
    position: relative;
    z-index: 1;
  }
  .stat-label {
    font-size: $font-md;
    opacity: 0.9;
    margin-bottom: $space-md;
    position: relative;
    z-index: 1;
  }
  .stat-bar {
    height: 6px;
    background: rgba(255, 255, 255, 0.25);
    border-radius: $radius-full;
    overflow: hidden;
    position: relative;
    z-index: 1;
  }
  .stat-bar-fill {
    height: 100%;
    background: white;
    border-radius: $radius-full;
    transition: width 0.6s cubic-bezier(0.4, 0, 0.2, 1);
  }
}

.turn-stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: $space-md;
  margin-bottom: $space-lg;
}

.turn-card {
  background: $bg-card;
  border: 1px solid $border-light;
  border-radius: $radius-lg;
  padding: $space-lg;
  text-align: center;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background: radial-gradient(circle at top right, rgba(59,130,246,0.1), transparent 60%);
    pointer-events: none;
  }
  &.turn-right::before {
    background: radial-gradient(circle at top right, rgba(139,92,246,0.1), transparent 60%);
  }

  .turn-icon {
    font-size: 32px;
    color: $color-cyan;
    margin-bottom: 4px;
    line-height: 1;
  }
  &.turn-right .turn-icon { color: $color-purple; }

  .turn-num {
    font-size: 36px;
    font-weight: 700;
    color: $text-primary;
    line-height: 1.1;
  }
  .turn-label {
    font-size: $font-xs;
    color: $text-secondary;
    margin-top: 4px;
  }
}

// ====== 操作按钮 ======
.actions {
  display: flex;
  flex-direction: column;
  gap: $space-md;
}

.primary-btn, .secondary-btn {
  width: 100%;
  height: 52px;
  border-radius: $radius-md;
  font-size: $font-lg;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $space-sm;
  transition: transform 0.15s;

  &:active { transform: scale(0.98); }
}

.primary-btn {
  background: $gradient-primary;
  color: white;
  box-shadow: $shadow-glow;
}

.secondary-btn {
  background: $bg-card;
  color: $text-primary;
  border: 1px solid $border-base;
}

// ====== 失败卡片 ======
.failed-card {
  background: $bg-card;
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: $radius-lg;
  padding: $space-2xl $space-lg;
  text-align: center;

  .failed-icon { font-size: 48px; margin-bottom: $space-md; }
  .failed-title {
    font-size: $font-lg;
    font-weight: 600;
    color: $color-danger;
    margin-bottom: $space-sm;
  }
  .failed-message {
    font-size: $font-sm;
    color: $text-regular;
    background: rgba(239, 68, 68, 0.1);
    padding: $space-md;
    border-radius: $radius-md;
    margin-bottom: $space-md;
    word-break: break-all;
  }
  .failed-tip {
    font-size: $font-xs;
    color: $text-secondary;
  }
}

.loading-skeleton {
  padding: $space-md $space-lg;
}
</style>
