<template>
  <div class="video-list-page">
    <!-- 顶部:背景渐变 -->
    <header class="hero">
      <div class="hero-bg">
        <div class="orb orb-1"></div>
        <div class="orb orb-2"></div>
      </div>
      <div class="hero-content">
        <h2>我的视频</h2>
      </div>
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
        <span v-if="tab.dotColor" class="tab-dot" :class="tab.dotColor"></span>
        {{ tab.label }} {{ getTabCount(tab.value) }}
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
            <!-- 已完成标记 -->
            <div v-if="v.analysisStatus === 'analyzed'" class="completed-badge">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor">
                <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z" />
              </svg>
            </div>
            <div class="thumb-icon">⛷️</div>
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

            <!-- 分析中进度 -->
            <div v-if="v.analysisStatus === 'analyzing' || v.analysisStatus === 'pending'" class="progress-bar-wrap">
              <div class="progress-text">AI 分析中...</div>
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: `${getProgress(v)}%` }"></div>
              </div>
              <div class="progress-percent">{{ getProgress(v) }}%</div>
            </div>

            <!-- 已完成的视频显示统计指标 -->
            <div v-if="v.analysisStatus === 'analyzed'" class="video-stats">
              <span class="stat-tag">检测 {{ formatPercent(v.detectionRate) }}</span>
              <span class="stat-item">↺ {{ v.turnLeftCount || 0 }}</span>
              <span class="stat-item">↻ {{ v.turnRightCount || 0 }}</span>
            </div>
          </div>

          <!-- 评分 -->
          <div v-if="v.analysisStatus === 'analyzed'" class="video-score">
            {{ calculateScore(v) }}
          </div>

          <!-- 箭头指示 -->
          <div class="video-arrow">›</div>
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
import { ref, computed, onMounted } from 'vue'
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
  { value: '', label: '全部', dotColor: '' },
  { value: 'analyzed', label: '已完成', dotColor: 'dot-green' },
  { value: 'analyzing', label: '分析中', dotColor: 'dot-orange' }
]

// 计算各状态数量
const analyzedCount = computed(() => list.value.filter(v => v.analysisStatus === 'analyzed').length)
const analyzingCount = computed(() => list.value.filter(v => v.analysisStatus === 'analyzing' || v.analysisStatus === 'pending').length)

function getTabCount(status) {
  if (!status) return total.value
  if (status === 'analyzed') return analyzedCount.value
  if (status === 'analyzing') return analyzingCount.value
  return 0
}

// 视频卡片用 5 种渐变色循环展示,避免单调
function gradientId(id) {
  return id % 5
}

function getProgress(v) {
  if (v.analysisStatus === 'pending') return 10
  if (v.analysisStatus === 'analyzing') return 64
  return 100
}

function calculateScore(v) {
  const detectionRate = v.detectionRate || 0
  const turns = (v.turnLeftCount || 0) + (v.turnRightCount || 0)
  const baseScore = Math.round(detectionRate * 100 * 0.7)
  const turnBonus = Math.min(turns * 3, 30)
  return baseScore + turnBonus
}

function handleSearch() {}
function toggleViewMode() {}

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
}

// ====== 顶部Hero ======
.hero {
  position: relative;
  padding: calc(20px + #{$safe-top}) $space-lg $space-3xl;
  overflow: hidden;
  background: linear-gradient(180deg, #DBEAFE 0%, $bg-base 100%);
}

.hero-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;

  .orb {
    position: absolute;
    border-radius: 50%;
    filter: blur(50px);
  }
  .orb-1 {
    top: -60px; right: -40px;
    width: 200px; height: 200px;
    background: radial-gradient(circle, rgba(139,92,246,0.5) 0%, transparent 70%);
  }
  .orb-2 {
    top: 40px; left: -60px;
    width: 180px; height: 180px;
    background: radial-gradient(circle, rgba(59,130,246,0.4) 0%, transparent 70%);
  }
}

.hero-content {
  position: relative;
  z-index: 1;

  h2 {
    font-size: $font-4xl;
    font-weight: 700;
    color: $text-primary;
    letter-spacing: -0.5px;
  }
}

// 状态筛选
.filter-tabs {
  display: flex;
  gap: $space-sm;
  background: #f0f0f0;
  margin: 0 $space-lg $space-md;
  padding: 4px;
  border-radius: $radius-lg;
  border: 1px solid $border-light;
}

.filter-tab {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 8px;
  background: transparent;
  border-radius: $radius-md;
  font-size: $font-sm;
  color: $text-regular;
  cursor: pointer;
  transition: all 0.15s;

  &.active {
    background: white;
    color: $text-primary;
    font-weight: 600;
    box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  }

  .tab-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;

    &.dot-green { background: $color-success; }
    &.dot-orange { background: $color-warning; }
  }

  .tab-count {
    font-weight: 600;
    color: $text-primary;
  }
}

.content {
  padding: 0 $space-lg;
}

// ====== 视频卡片 ======
.video-card {
  display: flex;
  align-items: center;
  gap: $space-md;
  background: $bg-card;
  border: 1px solid $border-light;
  border-radius: $radius-lg;
  margin-bottom: $space-md;
  padding: $space-md;
  cursor: pointer;
  transition: transform 0.15s;

  &:active { transform: scale(0.99); }
}

.video-thumb {
  position: relative;
  width: 80px;
  height: 60px;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;

  // 5 种渐变背景循环
  &.thumb-0 { background: linear-gradient(135deg, #4c1d95 0%, #7e22ce 100%); }
  &.thumb-1 { background: linear-gradient(135deg, #075985 0%, #0c4a6e 100%); }
  &.thumb-2 { background: linear-gradient(135deg, #4c1d95 0%, #7e22ce 100%); }
  &.thumb-3 { background: linear-gradient(135deg, #134e4a 0%, #0f766e 100%); }
  &.thumb-4 { background: linear-gradient(135deg, #831843 0%, #be185d 100%); }

  // 添加细微噪点纹理感
  &::after {
    content: '';
    position: absolute;
    inset: 0;
    background: radial-gradient(circle at 30% 30%, rgba(255,255,255,0.1) 0%, transparent 60%);
    pointer-events: none;
  }
}

.completed-badge {
  position: absolute;
  top: 4px;
  left: 4px;
  width: 20px;
  height: 20px;
  background: $color-success;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.thumb-icon {
  font-size: 24px;
  filter: drop-shadow(0 4px 8px rgba(0,0,0,0.3));
}

.duration-badge {
  position: absolute;
  bottom: 2px;
  right: 4px;
  padding: 2px 6px;
  background: rgba(0, 0, 0, 0.6);
  border-radius: 4px;
  font-size: 10px;
  color: white;
}

.video-info {
  flex: 1;
  min-width: 0;
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
  margin-bottom: $space-xs;

  .dot { margin: 0 4px; }
}

// 进度条
.progress-bar-wrap {
  display: flex;
  align-items: center;
  gap: $space-xs;
}

.progress-text {
  font-size: $font-xs;
  color: $text-secondary;
  flex-shrink: 0;
}

.progress-bar {
  flex: 1;
  height: 4px;
  background: rgba(0,0,0,0.1);
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, $color-primary 0%, $color-purple 100%);
  border-radius: 2px;
  transition: width 0.3s;
}

.progress-percent {
  font-size: $font-xs;
  font-weight: 600;
  color: $color-warning;
  flex-shrink: 0;
  min-width: 32px;
  text-align: right;
}

.video-stats {
  display: flex;
  align-items: center;
  gap: $space-sm;
}

.stat-tag {
  font-size: $font-xs;
  color: $color-cyan;
  font-weight: 500;
  padding: 2px 6px;
  background: rgba(6, 182, 212, 0.1);
  border-radius: 4px;
}

.stat-item {
  font-size: $font-xs;
  color: $text-secondary;
}

.video-score {
  font-size: 24px;
  font-weight: 700;
  color: $color-primary;
  flex-shrink: 0;
}

.video-arrow {
  font-size: 20px;
  color: $text-secondary;
  line-height: 1;
  flex-shrink: 0;
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
  box-shadow: $shadow-glow, 0 8px 24px rgba(59,130,246,0.4);
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