<template>
  <div class="page">
    <header class="nav-bar">
      <button class="back-btn" @click="router.back()">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
          <path d="M15 19L8 12l7-7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
      </button>
      <h1 class="nav-title">创建对比</h1>
      <div class="nav-spacer"></div>
    </header>

    <div class="intro-card">
      <div class="intro-emoji">📊</div>
      <div class="intro-text">
        <div class="intro-title">看到自己的进步</div>
        <div class="intro-desc">选两个已分析的视频,AI 教练会逐项对比</div>
      </div>
    </div>

    <div class="selection-row">
      <div class="selection-slot" :class="{ filled: prevVideo, active: !prevVideo }">
        <div v-if="prevVideo" class="slot-thumb" :class="`thumb-${prevVideo.id % 5}`">
          <div class="slot-icon">🎿</div>
        </div>
        <div v-else class="slot-empty">
          <div class="slot-num">1</div>
          <div class="slot-hint">上次</div>
        </div>
        <div v-if="prevVideo" class="slot-info">
          <div class="slot-title">上次</div>
          <div class="slot-name">{{ prevVideo.originalFilename }}</div>
        </div>
      </div>

      <div class="vs-badge">VS</div>

      <div class="selection-slot" :class="{ filled: currVideo, active: prevVideo && !currVideo }">
        <div v-if="currVideo" class="slot-thumb" :class="`thumb-${currVideo.id % 5}`">
          <div class="slot-icon">🎿</div>
        </div>
        <div v-else class="slot-empty">
          <div class="slot-num">2</div>
          <div class="slot-hint">本次</div>
        </div>
        <div v-if="currVideo" class="slot-info">
          <div class="slot-title">本次</div>
          <div class="slot-name">{{ currVideo.originalFilename }}</div>
        </div>
      </div>
    </div>

    <div v-if="!prevVideo" class="step-hint">👇 先选"上次"的视频(对比基准)</div>
    <div v-else-if="!currVideo" class="step-hint">👇 再选"本次"的视频(用来对比)</div>
    <div v-else class="step-hint ready">✅ 都选好了!点击下方按钮生成对比</div>

    <div class="list-section">
      <div class="list-header">
        <h3>已分析完成的视频</h3>
        <span class="list-count">{{ availableVideos.length }} 个</span>
      </div>

      <div v-if="loading" class="skeleton-list">
        <div v-for="i in 3" :key="i" class="card-skeleton skeleton"></div>
      </div>

      <div v-else-if="!availableVideos.length" class="empty">
        <div class="empty-emoji">📼</div>
        <div class="empty-text">至少需要 2 个分析完成的视频</div>
        <button class="empty-btn" @click="router.push('/videos')">
          去上传视频
        </button>
      </div>

      <div v-else class="video-list">
        <div
          v-for="v in availableVideos"
          :key="v.id"
          class="video-item"
          :class="{ selected: isSelected(v) }"
          @click="onVideoTap(v)"
        >
          <div class="item-thumb" :class="`thumb-${v.id % 5}`">
            <div class="thumb-icon">🎿</div>
            <div v-if="isSelected(v)" class="selected-badge">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                <path d="M5 13l4 4L19 7" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
            </div>
          </div>
          <div class="item-info">
            <div class="item-name">{{ v.originalFilename }}</div>
            <div class="item-meta">
              <span>{{ formatDate(v.createdTime) }}</span>
              <span class="dot">·</span>
              <span>检测率 {{ formatPercent(v.detectionRate) }}</span>
            </div>
          </div>
          <div v-if="isSelected(v)" class="item-tag" :class="getRoleClass(v)">
            {{ getRoleText(v) }}
          </div>
        </div>
      </div>
    </div>

    <div class="sticky-bottom" :class="{ visible: prevVideo && currVideo }">
      <button
        class="primary-btn"
        :disabled="!canSubmit"
        :class="{ loading: submitting }"
        @click="handleSubmit"
      >
        <span v-if="!submitting" class="btn-content">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
            <path d="M13 10V3L4 14h7v7l9-11h-7z" stroke="white" stroke-width="2" stroke-linejoin="round" />
          </svg>
          立即生成对比报告
        </span>
        <span v-else class="loading-dots"><span></span><span></span><span></span></span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showFailToast, showSuccessToast } from 'vant'
import { listVideos } from '@/api/video'
import { createComparison } from '@/api/comparison'
import { formatDate, formatPercent } from '@/utils/format'

const router = useRouter()
const route = useRoute()

const allVideos = ref([])
const loading = ref(true)
const submitting = ref(false)

const prevVideo = ref(null)
const currVideo = ref(null)

const availableVideos = computed(() => {
  return allVideos.value.filter(v => v.analysisStatus === 'analyzed')
})

const canSubmit = computed(() => prevVideo.value && currVideo.value && !submitting.value)

onMounted(async () => {
  loading.value = true
  try {
    const data = await listVideos({ pageNum: 1, pageSize: 50, analysisStatus: 'analyzed' })
    allVideos.value = data.records || []

    const fromId = route.query.from
    if (fromId) {
      const v = allVideos.value.find(v => String(v.id) === String(fromId))
      if (v) currVideo.value = v
    }
  } finally {
    loading.value = false
  }
})

function isSelected(v) {
  return prevVideo.value?.id === v.id || currVideo.value?.id === v.id
}

function getRoleClass(v) {
  if (prevVideo.value?.id === v.id) return 'role-prev'
  if (currVideo.value?.id === v.id) return 'role-curr'
  return ''
}

function getRoleText(v) {
  if (prevVideo.value?.id === v.id) return '上次'
  if (currVideo.value?.id === v.id) return '本次'
  return ''
}

function onVideoTap(v) {
  if (prevVideo.value?.id === v.id) {
    prevVideo.value = null
    return
  }
  if (currVideo.value?.id === v.id) {
    currVideo.value = null
    return
  }
  if (!prevVideo.value) {
    prevVideo.value = v
  } else if (!currVideo.value) {
    currVideo.value = v
  } else {
    showFailToast('已选两个,请先取消再选')
  }
}

async function handleSubmit() {
  if (!canSubmit.value) return
  submitting.value = true

  try {
    const data = await createComparison({
      prevVideoId: prevVideo.value.id,
      currVideoId: currVideo.value.id
    })

    if (data.cacheHit) {
      showSuccessToast('该视频对已有报告')
      router.replace(`/comparisons/${data.reportId}`)
    } else {
      router.replace({
        path: '/comparison/waiting',
        query: { taskId: data.taskId }
      })
    }
  } catch {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: $bg-base;
  padding-bottom: calc(80px + #{$safe-bottom});
}

.nav-bar {
  position: sticky;
  top: 0;
  z-index: $z-navbar;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px $space-md;
  padding-top: calc(12px + #{$safe-top});
  background: rgba(10, 14, 39, 0.85);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid $border-light;

  .back-btn {
    width: 40px; height: 40px;
    display: flex; align-items: center; justify-content: center;
    color: $text-primary;
  }
  .nav-title {
    font-size: $font-md;
    font-weight: 600;
    color: $text-primary;
  }
  .nav-spacer { width: 40px; }
}

.intro-card {
  display: flex;
  align-items: center;
  gap: $space-md;
  margin: $space-md $space-lg 0;
  padding: $space-md $space-lg;
  background: $glass-bg;
  border: $glass-border;
  border-radius: $radius-lg;

  .intro-emoji { font-size: 32px; flex-shrink: 0; }
  .intro-title {
    font-size: $font-md;
    font-weight: 600;
    color: $text-primary;
  }
  .intro-desc {
    font-size: $font-xs;
    color: $text-secondary;
    margin-top: 2px;
  }
}

.selection-row {
  display: flex;
  align-items: center;
  gap: $space-sm;
  padding: $space-lg;
}

.selection-slot {
  flex: 1;
  background: $bg-card;
  border: 1.5px solid $border-light;
  border-radius: $radius-lg;
  padding: $space-md;
  height: 130px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: $space-sm;
  transition: all 0.2s;
  position: relative;
  overflow: hidden;

  &.active {
    border-color: $color-primary;
    box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.15);
    animation: gentle-pulse 2s infinite;
  }
  &.filled {
    border-color: rgba(255, 255, 255, 0.2);
  }
}

@keyframes gentle-pulse {
  0%, 100% { box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.15); }
  50% { box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.25); }
}

.slot-thumb {
  width: 56px;
  height: 56px;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  &.thumb-0 { background: linear-gradient(135deg, #1e3a8a, #6d28d9); }
  &.thumb-1 { background: linear-gradient(135deg, #075985, #0c4a6e); }
  &.thumb-2 { background: linear-gradient(135deg, #4c1d95, #7e22ce); }
  &.thumb-3 { background: linear-gradient(135deg, #134e4a, #0f766e); }
  &.thumb-4 { background: linear-gradient(135deg, #831843, #be185d); }
}

.slot-icon { font-size: 28px; }

.slot-empty { text-align: center; }

.slot-num {
  width: 36px;
  height: 36px;
  margin: 0 auto 4px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.06);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: $font-lg;
  font-weight: 600;
  color: $text-secondary;
}

.slot-hint {
  font-size: $font-xs;
  color: $text-secondary;
}

.slot-info {
  text-align: center;
  width: 100%;
}

.slot-title {
  font-size: $font-xs;
  color: $color-cyan;
  margin-bottom: 2px;
  font-weight: 500;
}

.slot-name {
  font-size: $font-xs;
  color: $text-primary;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.vs-badge {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: $gradient-primary;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: $font-xs;
  font-weight: 700;
  letter-spacing: 0.5px;
  flex-shrink: 0;
  box-shadow: $shadow-glow;
}

.step-hint {
  text-align: center;
  font-size: $font-sm;
  color: $color-cyan;
  padding: 0 $space-lg $space-md;

  &.ready {
    color: $color-success;
    font-weight: 500;
  }
}

.list-section {
  padding: 0 $space-lg;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: $space-md;

  h3 {
    font-size: $font-md;
    font-weight: 600;
    color: $text-primary;
  }
  .list-count {
    font-size: $font-xs;
    color: $text-secondary;
  }
}

.video-list {
  display: flex;
  flex-direction: column;
  gap: $space-sm;
}

.video-item {
  display: flex;
  align-items: center;
  padding: $space-md;
  background: $bg-card;
  border: 1.5px solid $border-light;
  border-radius: $radius-md;
  cursor: pointer;
  transition: all 0.15s;

  &:active { transform: scale(0.99); }

  &.selected {
    border-color: $color-primary;
    background: rgba(59, 130, 246, 0.08);
    box-shadow: 0 0 0 1px rgba(59, 130, 246, 0.3);
  }
}

.item-thumb {
  width: 48px;
  height: 48px;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  position: relative;

  &.thumb-0 { background: linear-gradient(135deg, #1e3a8a, #6d28d9); }
  &.thumb-1 { background: linear-gradient(135deg, #075985, #0c4a6e); }
  &.thumb-2 { background: linear-gradient(135deg, #4c1d95, #7e22ce); }
  &.thumb-3 { background: linear-gradient(135deg, #134e4a, #0f766e); }
  &.thumb-4 { background: linear-gradient(135deg, #831843, #be185d); }
}

.thumb-icon { font-size: 24px; }

.selected-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: $color-primary;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid $bg-base;
}

.item-info {
  flex: 1;
  min-width: 0;
  margin-left: $space-md;
  margin-right: $space-sm;
}

.item-name {
  font-size: $font-sm;
  color: $text-primary;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-meta {
  font-size: $font-xs;
  color: $text-secondary;
  margin-top: 2px;

  .dot { margin: 0 4px; }
}

.item-tag {
  padding: 4px 8px;
  border-radius: $radius-sm;
  font-size: $font-xs;
  font-weight: 600;
  flex-shrink: 0;

  &.role-prev {
    background: rgba(6, 182, 212, 0.15);
    color: $color-cyan;
  }
  &.role-curr {
    background: rgba(139, 92, 246, 0.15);
    color: $color-purple;
  }
}

.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: $space-sm;
}

.card-skeleton {
  height: 76px;
  border-radius: $radius-md;
}

.empty {
  text-align: center;
  padding: $space-3xl 0;

  .empty-emoji { font-size: 48px; margin-bottom: $space-md; opacity: 0.5; }
  .empty-text { color: $text-secondary; margin-bottom: $space-lg; }
  .empty-btn {
    padding: 10px 24px;
    background: $gradient-primary;
    color: white;
    border-radius: $radius-full;
    font-size: $font-sm;
    box-shadow: $shadow-glow;
  }
}

.sticky-bottom {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: $space-md $space-lg;
  padding-bottom: calc(#{$space-md} + #{$safe-bottom});
  background: linear-gradient(180deg, rgba(10, 14, 39, 0) 0%, rgba(10, 14, 39, 0.95) 30%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  pointer-events: none;
  opacity: 0.5;
  transition: opacity 0.3s;
  z-index: 99;

  &.visible {
    opacity: 1;
    pointer-events: auto;
  }
}

.primary-btn {
  width: 100%;
  height: 52px;
  background: $gradient-primary;
  border-radius: $radius-md;
  color: white;
  font-size: $font-lg;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: $shadow-glow;
  transition: transform 0.15s;

  .btn-content {
    display: flex;
    align-items: center;
    gap: $space-sm;
  }

  &:active:not(:disabled) { transform: scale(0.98); }
  &:disabled { opacity: 0.4; cursor: not-allowed; }
  &.loading { opacity: 0.85; }
}

.loading-dots {
  display: inline-flex;
  gap: 4px;
  span {
    width: 6px; height: 6px;
    background: white;
    border-radius: 50%;
    animation: dots 1.4s infinite ease-in-out both;
    &:nth-child(1) { animation-delay: -0.32s; }
    &:nth-child(2) { animation-delay: -0.16s; }
  }
}
@keyframes dots {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}
</style>
