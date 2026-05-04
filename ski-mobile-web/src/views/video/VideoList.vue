<template>
  <div class="video-list-page">
    <!-- 顶栏 -->
    <header class="page-header">
      <h2>我的视频</h2>
      <p class="header-sub">{{ total }} 个视频</p>
    </header>

    <!-- 状态筛选 -->
    <div class="filter-tabs">
      <div
        v-for="tab in filterTabs"
        :key="tab.value"
        class="filter-tab"
        :class="{ active: filterStatus === tab.value }"
        @click="changeFilter(tab.value)"
      >
        {{ tab.label }}
      </div>
    </div>

    <!-- 列表 / 加载 / 空状态 -->
    <div class="content">
      <!-- 加载骨架 -->
      <div v-if="loading && !list.length" class="skeleton-list">
        <div v-for="i in 3" :key="i" class="card-skeleton skeleton"></div>
      </div>

      <!-- 空状态 -->
      <div v-else-if="!list.length" class="empty">
        <div class="empty-emoji">🎬</div>
        <div class="empty-title">还没有视频</div>
        <div class="empty-desc">上传你的第一个滑雪视频</div>
      </div>

      <!-- 视频卡片列表 -->
      <van-list
        v-else
        v-model:loading="loadingMore"
        :finished="finished"
        :immediate-check="false"
        finished-text="没有更多了"
        @load="loadMore"
      >
        <div
          v-for="v in list"
          :key="v.id"
          class="video-card"
          @click="goDetail(v)"
        >
          <!-- 缩略图区(渐变占位) -->
          <div class="video-thumb" :class="`thumb-${gradientId(v.id)}`">
            <div class="thumb-icon">🎿</div>
            <!-- 状态徽章 -->
            <div class="status-badge" :class="`status-${v.analysisStatus}`">
              <span class="status-dot"></span>
              {{ VIDEO_STATUS[v.analysisStatus]?.text }}
            </div>
            <!-- 时长 -->
            <div v-if="v.durationSeconds" class="duration-badge">
              {{ formatDuration(v.durationSeconds) }}
            </div>
          </div>

          <!-- 信息区 -->
          <div class="video-info">
            <div class="video-name">{{ v.originalFilename }}</div>
            <div class="video-meta">
              <span>{{ formatFileSize(v.fileSize) }}</span>
              <span class="dot">·</span>
              <span>{{ formatDate(v.createdTime) }}</span>
            </div>

            <!-- 已完成的视频显示统计指标 -->
            <div v-if="v.analysisStatus === 'analyzed'" class="video-stats">
              <div class="stat">
                <span class="label">检测率</span>
                <span class="value">{{ formatPercent(v.detectionRate) }}</span>
              </div>
              <div class="stat">
                <span class="label">左转</span>
                <span class="value">{{ v.turnLeftCount || 0 }}</span>
              </div>
              <div class="stat">
                <span class="label">右转</span>
                <span class="value">{{ v.turnRightCount || 0 }}</span>
              </div>
            </div>
          </div>
        </div>
      </van-list>
    </div>

    <!-- 浮动上传按钮 -->
    <div class="fab" @click="triggerUpload">
      <input
        ref="fileInput"
        type="file"
        accept="video/mp4,video/quicktime,.mp4,.mov,.m4v"
        class="hidden-input"
        @change="handleFileChange"
      />
      <svg width="28" height="28" viewBox="0 0 24 24" fill="none">
        <path d="M12 5v14M5 12h14" stroke="white" stroke-width="2.5" stroke-linecap="round" />
      </svg>
    </div>

    <!-- 上传进度弹层 -->
    <van-popup
      v-model:show="showUploadModal"
      :close-on-click-overlay="false"
      position="center"
      round
      :style="{ background: 'transparent', overflow: 'visible' }"
    >
      <div class="upload-modal">
        <div class="upload-icon-wrap">
          <div class="ring-progress" :style="{ '--p': uploadProgress }">
            <svg viewBox="0 0 100 100">
              <circle class="ring-bg" cx="50" cy="50" r="42" />
              <circle class="ring-fill" cx="50" cy="50" r="42"
                      :stroke-dasharray="263.89"
                      :stroke-dashoffset="263.89 - (263.89 * uploadProgress / 100)" />
            </svg>
            <div class="ring-text">{{ uploadProgress }}%</div>
          </div>
        </div>
        <div class="upload-title">{{ uploadStatus }}</div>
        <div class="upload-subtitle">{{ uploadingFile?.name }}</div>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showSuccessToast, showFailToast, showDialog } from 'vant'
import { listVideos, uploadVideo } from '@/api/video'
import { VIDEO_STATUS, MAX_FILE_SIZE_MB, ALLOWED_VIDEO_EXTENSIONS } from '@/utils/constants'
import { formatFileSize, formatDate, formatDuration, formatPercent } from '@/utils/format'

const router = useRouter()

const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const filterStatus = ref('')
const loading = ref(false)
const loadingMore = ref(false)
const finished = ref(false)

const filterTabs = [
  { value: '', label: '全部' },
  { value: 'analyzed', label: '已完成' },
  { value: 'analyzing', label: '分析中' },
  { value: 'pending', label: '排队中' },
  { value: 'failed', label: '失败' }
]

// 视频卡片用 5 种渐变色循环展示,避免单调
function gradientId(id) {
  return id % 5
}

onMounted(() => loadList(true))

async function loadList(reset = false) {
  if (reset) {
    pageNum.value = 1
    finished.value = false
    list.value = []
  }
  loading.value = true
  try {
    const data = await listVideos({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      analysisStatus: filterStatus.value || undefined
    })
    if (reset) {
      list.value = data.records || []
    } else {
      list.value.push(...(data.records || []))
    }
    total.value = data.total || 0
    if (list.value.length >= total.value) {
      finished.value = true
    }
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

async function loadMore() {
  if (finished.value) return
  pageNum.value++
  await loadList(false)
}

function changeFilter(val) {
  filterStatus.value = val
  loadList(true)
}

// ====== 上传 ======
const fileInput = ref()
const showUploadModal = ref(false)
const uploadProgress = ref(0)
const uploadingFile = ref(null)
const uploadStatus = ref('准备中...')

function triggerUpload() {
  fileInput.value?.click()
}

async function handleFileChange(e) {
  const file = e.target.files?.[0]
  e.target.value = ''   // 清空,允许重复选同一个
  if (!file) return

  // 校验
  const ext = file.name.split('.').pop()?.toLowerCase()
  if (!ALLOWED_VIDEO_EXTENSIONS.includes(ext)) {
    showFailToast(`只支持 ${ALLOWED_VIDEO_EXTENSIONS.join(', ')}`)
    return
  }
  if (file.size / 1024 / 1024 > MAX_FILE_SIZE_MB) {
    showFailToast(`文件不能超过 ${MAX_FILE_SIZE_MB}MB`)
    return
  }

  uploadingFile.value = file
  uploadProgress.value = 0
  uploadStatus.value = '正在上传...'
  showUploadModal.value = true

  try {
    const data = await uploadVideo(file, (p) => {
      uploadProgress.value = p
      if (p >= 100) uploadStatus.value = '上传完成,处理中...'
    })

    showUploadModal.value = false

    if (data.instantUpload) {
      showSuccessToast('秒传成功')
      router.push(`/videos/${data.videoId}`)
    } else {
      showSuccessToast('上传成功,AI 正在分析')
      // 跳转到视频详情页,在那里轮询进度
      setTimeout(() => {
        router.push(`/videos/${data.videoId}`)
      }, 600)
    }
  } catch (e) {
    showUploadModal.value = false
  }
}

function goDetail(v) {
  router.push(`/videos/${v.id}`)
}
</script>

<style lang="scss" scoped>
.video-list-page {
  min-height: 100vh;
  background: $bg-base;
  padding-bottom: $space-2xl;
}

.page-header {
  padding: calc(20px + #{$safe-top}) $space-lg $space-md;

  h2 {
    font-size: $font-4xl;
    font-weight: 700;
    color: $text-primary;
    letter-spacing: -0.5px;
  }
  .header-sub {
    color: $text-secondary;
    font-size: $font-sm;
    margin-top: 4px;
  }
}

// 状态筛选
.filter-tabs {
  display: flex;
  gap: $space-sm;
  padding: 0 $space-lg $space-md;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;

  &::-webkit-scrollbar { display: none; }
}

.filter-tab {
  flex-shrink: 0;
  padding: 6px 16px;
  background: $bg-card;
  border: 1px solid $border-light;
  border-radius: $radius-full;
  font-size: $font-sm;
  color: $text-regular;
  cursor: pointer;
  transition: all 0.15s;

  &.active {
    background: $gradient-primary;
    color: white;
    border-color: transparent;
    box-shadow: $shadow-glow;
  }
}

.content {
  padding: 0 $space-lg;
}

// ====== 视频卡片 ======
.video-card {
  background: $bg-card;
  border: 1px solid $border-light;
  border-radius: $radius-lg;
  margin-bottom: $space-md;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.15s;

  &:active { transform: scale(0.99); }
}

.video-thumb {
  position: relative;
  aspect-ratio: 16/9;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;

  // 5 种渐变背景循环
  &.thumb-0 { background: linear-gradient(135deg, #0C4A6E 0%, #0891B2 100%); }
  &.thumb-1 { background: linear-gradient(135deg, #0369A1 0%, #0284C7 100%); }
  &.thumb-2 { background: linear-gradient(135deg, #1E40AF 0%, #2563EB 100%); }
  &.thumb-3 { background: linear-gradient(135deg, #155E75 0%, #0E7490 100%); }
  &.thumb-4 { background: linear-gradient(135deg, #0F766E 0%, #14B8A6 100%); }

  // 添加细微噪点纹理感
  &::after {
    content: '';
    position: absolute;
    inset: 0;
    background: radial-gradient(circle at 30% 30%, rgba(255,255,255,0.1) 0%, transparent 60%);
    pointer-events: none;
  }
}

.thumb-icon {
  font-size: 56px;
  filter: drop-shadow(0 4px 12px rgba(0,0,0,0.3));
}

.status-badge {
  position: absolute;
  top: $space-md;
  left: $space-md;
  padding: 4px 10px;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(10px);
  border-radius: $radius-full;
  font-size: $font-xs;
  color: white;
  display: flex;
  align-items: center;
  gap: 4px;

  .status-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #6B7280;
  }
  &.status-analyzed .status-dot { background: $color-success; box-shadow: 0 0 8px $color-success; }
  &.status-analyzing .status-dot { background: $color-warning; animation: pulse 1.4s infinite; }
  &.status-pending .status-dot { background: $color-info; }
  &.status-failed .status-dot { background: $color-danger; }
}

.duration-badge {
  position: absolute;
  bottom: $space-md;
  right: $space-md;
  padding: 2px 8px;
  background: rgba(0, 0, 0, 0.6);
  border-radius: $radius-sm;
  font-size: $font-xs;
  color: white;
}

.video-info {
  padding: $space-md $space-lg;
}

.video-name {
  font-size: $font-md;
  font-weight: 500;
  color: $text-primary;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.video-meta {
  font-size: $font-xs;
  color: $text-secondary;
  margin-bottom: $space-sm;

  .dot { margin: 0 4px; }
}

.video-stats {
  display: flex;
  gap: $space-md;
  padding-top: $space-sm;
  border-top: 1px dashed $border-light;
}

.stat {
  flex: 1;

  .label {
    font-size: $font-xs;
    color: $text-secondary;
    margin-right: 4px;
  }
  .value {
    font-size: $font-sm;
    font-weight: 600;
    color: $color-cyan;
  }
}

// ====== 浮动上传按钮 ======
.fab {
  position: fixed;
  right: 20px;
  bottom: calc(80px + #{$safe-bottom});
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: $gradient-primary;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: $shadow-glow, 0 8px 24px rgba(14,165,233,0.4);
  z-index: 99;
  transition: transform 0.15s;

  &:active { transform: scale(0.92); }

  .hidden-input {
    display: none;
  }
}

// ====== 上传弹层 ======
.upload-modal {
  width: 280px;
  padding: $space-3xl $space-2xl;
  background: $bg-card;
  border: 1px solid $border-light;
  border-radius: $radius-2xl;
  text-align: center;
  box-shadow: $shadow-lg;
}

.ring-progress {
  width: 100px;
  height: 100px;
  position: relative;
  margin: 0 auto $space-lg;

  svg {
    width: 100%;
    height: 100%;
    transform: rotate(-90deg);
  }

  .ring-bg {
    fill: none;
    stroke: $border-light;
    stroke-width: 6;
  }
  .ring-fill {
    fill: none;
    stroke: url(#tabGradient);
    stroke-width: 6;
    stroke-linecap: round;
    transition: stroke-dashoffset 0.3s;
  }

  .ring-text {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: $font-2xl;
    font-weight: 700;
    background: $gradient-primary;
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
  }
}

.upload-title {
  font-size: $font-lg;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 4px;
}

.upload-subtitle {
  font-size: $font-sm;
  color: $text-secondary;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

// ====== 加载骨架 ======
.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: $space-md;
}
.card-skeleton {
  height: 220px;
  border-radius: $radius-lg;
}

// ====== 空状态 ======
.empty {
  text-align: center;
  padding: 80px 0;

  .empty-emoji {
    font-size: 64px;
    margin-bottom: $space-md;
    opacity: 0.5;
  }
  .empty-title {
    font-size: $font-lg;
    color: $text-regular;
    margin-bottom: 4px;
    font-weight: 500;
  }
  .empty-desc {
    font-size: $font-sm;
    color: $text-secondary;
  }
}
</style>
