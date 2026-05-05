<template>
  <div class="page">
    <header class="nav-bar">
      <button class="back-btn" @click="router.back()">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
          <path d="M15 19L8 12l7-7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
      </button>
      <h1 class="nav-title">反馈详情</h1>
      <div class="nav-spacer"></div>
    </header>

    <div v-if="loading" class="loading-wrap">
      <div class="skeleton" style="height: 120px; margin: 16px; border-radius: 12px;"></div>
      <div class="skeleton" style="height: 200px; margin: 0 16px 16px; border-radius: 12px;"></div>
    </div>

    <div v-else-if="detail" class="content">
      <!-- 状态 banner -->
      <div class="status-banner" :class="`banner-status-${detail.status}`">
        <div class="banner-bg">
          <div class="orb orb-1"></div>
          <div class="orb orb-2"></div>
        </div>
        <div class="banner-content">
          <div class="banner-emoji">{{ statusEmoji[detail.status] }}</div>
          <div class="banner-status-text">{{ statusMap[detail.status] }}</div>
        </div>
      </div>

      <!-- 反馈内容卡片 -->
      <div class="info-card" :class="`stripe-${detail.type}`">
        <div class="card-header">
          <div class="card-meta">
            <span class="type-tag" :class="`tag-${detail.type}`">
              {{ typeMap[detail.type]?.label || detail.type }}
            </span>
            <span class="status-tag" :class="`status-${detail.status}`">
              {{ statusMap[detail.status] }}
            </span>
          </div>
          <span class="card-time">{{ detail.createdTime }}</span>
        </div>

        <div class="card-body">{{ detail.content }}</div>

        <!-- 图片 -->
        <div v-if="detail.imageCount" class="images-section">
          <div class="images-label">📷 {{ detail.imageCount }}张截图</div>
          <div class="images-grid">
            <div
              v-for="i in detail.imageCount"
              :key="i"
              class="image-item"
              @click="previewIndex = i - 1; showPreview = true"
            >
              <img
                :src="`/api/feedbacks/${detail.id}/images/${i - 1}?token=${userStore.token}`"
                :alt="`截图${i}`"
                loading="lazy"
              />
            </div>
          </div>
        </div>

        <div class="card-meta-row">
          <span v-if="detail.contact" class="meta-item">📞 {{ detail.contact }}</span>
          <span v-if="detail.appVersion" class="meta-item">v{{ detail.appVersion }}</span>
        </div>
      </div>

      <!-- 官方回复 -->
      <div v-if="detail.status === 2 && detail.reply" class="reply-card">
        <div class="reply-header">
          <div class="reply-badge">官方回复</div>
        </div>
        <div class="reply-body">{{ detail.reply }}</div>
      </div>
    </div>

    <!-- 图片预览 -->
    <van-image-preview
      v-model:show="showPreview"
      :images="previewUrls"
      :start-position="previewIndex"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getFeedback } from '@/api/feedback'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const typeMap = {
  bug: { label: 'BUG反馈' },
  feature: { label: '功能建议' },
  performance: { label: '测速反馈' },
  other: { label: '其他' }
}

const statusMap = ['待处理', '已查看', '已回复']
const statusEmoji = ['⏳', '👀', '✅']

const detail = ref(null)
const loading = ref(true)
const showPreview = ref(false)
const previewIndex = ref(0)

const previewUrls = computed(() => {
  if (!detail.value) return []
  return Array.from({ length: detail.value.imageCount }, (_, i) =>
    `/api/feedbacks/${detail.value.id}/images/${i}?token=${userStore.token}`
  )
})

onMounted(async () => {
  loading.value = true
  try {
    detail.value = await getFeedback(route.params.id)
  } finally {
    loading.value = false
  }
})
</script>

<style lang="scss" scoped>
.page {
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

// ====== 状态 banner ======
.status-banner {
  position: relative;
  margin: $space-md $space-lg;
  padding: $space-2xl;
  border-radius: $radius-xl;
  overflow: hidden;
  text-align: center;

  &.banner-status-0 { background: linear-gradient(135deg, #64748B 0%, #94A3B8 100%); }
  &.banner-status-1 { background: linear-gradient(135deg, #1E40AF 0%, #6366F1 50%, #8B5CF6 100%); }
  &.banner-status-2 { background: linear-gradient(135deg, #047857 0%, #10B981 100%); }
}

.banner-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;

  .orb {
    position: absolute;
    border-radius: 50%;
    filter: blur(40px);
  }
  .orb-1 {
    top: -30px; right: -20px;
    width: 100px; height: 100px;
    background: radial-gradient(circle, rgba(255,255,255,0.12) 0%, transparent 60%);
  }
  .orb-2 {
    bottom: -30px; left: -30px;
    width: 100px; height: 100px;
    background: radial-gradient(circle, rgba(255,255,255,0.08) 0%, transparent 60%);
  }
}

.banner-content {
  position: relative;
  z-index: 1;
}

.banner-emoji {
  font-size: 36px;
  margin-bottom: $space-sm;
}

.banner-status-text {
  font-size: $font-lg;
  font-weight: 600;
  color: white;
}

// ====== 内容卡片 ======
.content {
  padding: 0 $space-lg;
}

.info-card {
  background: $bg-card;
  border: 1px solid $border-light;
  border-radius: $radius-lg;
  padding: $space-lg;
  margin-bottom: $space-md;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    left: 0; top: 0; bottom: 0;
    width: 4px;
    border-radius: 0 2px 2px 0;
  }

  &.stripe-bug::before        { background: #EF4444; }
  &.stripe-feature::before    { background: $color-primary; }
  &.stripe-performance::before { background: #F59E0B; }
  &.stripe-other::before      { background: $text-secondary; }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $space-md;
  padding-bottom: $space-md;
  border-bottom: 1px solid $border-light;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: $space-sm;
}

.type-tag {
  font-size: $font-xs;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: $radius-sm;

  &.tag-bug          { background: rgba(239, 68, 68, 0.08); color: #DC2626; }
  &.tag-feature      { background: rgba(37, 99, 235, 0.08); color: #2563EB; }
  &.tag-performance  { background: rgba(245, 158, 11, 0.08); color: #D97706; }
  &.tag-other        { background: $bg-elevated; color: $text-secondary; }
}

.status-tag {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: $radius-sm;

  &.status-0 { color: $text-secondary; background: $bg-elevated; }
  &.status-1 { color: $color-primary; background: rgba(37, 99, 235, 0.06); }
  &.status-2 { color: $color-success; background: rgba(16, 185, 129, 0.06); }
}

.card-time {
  font-size: $font-xs;
  color: $text-secondary;
}

.card-body {
  font-size: $font-base;
  color: $text-primary;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-all;
}

// ====== 图片 ======
.images-section {
  margin-top: $space-lg;
  padding-top: $space-md;
  border-top: 1px solid $border-light;
}

.images-label {
  font-size: $font-xs;
  color: $text-secondary;
  margin-bottom: $space-md;
}

.images-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: $space-sm;
}

.image-item {
  aspect-ratio: 1;
  border-radius: $radius-md;
  overflow: hidden;
  cursor: pointer;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  &:active { opacity: 0.8; }
}

// ====== 元信息 ======
.card-meta-row {
  display: flex;
  gap: $space-lg;
  margin-top: $space-lg;
  padding-top: $space-md;
  border-top: 1px solid $border-light;
}

.meta-item {
  font-size: $font-xs;
  color: $text-secondary;
}

// ====== 官方回复 ======
.reply-card {
  background: linear-gradient(135deg, #ECFDF5 0%, #D1FAE5 100%);
  border: 1px solid rgba(16, 185, 129, 0.2);
  border-radius: $radius-lg;
  padding: $space-lg;
}

.reply-header {
  display: flex;
  align-items: center;
  margin-bottom: $space-md;
}

.reply-badge {
  font-size: $font-xs;
  font-weight: 600;
  color: $color-success;
  background: rgba(16, 185, 129, 0.12);
  padding: 4px 10px;
  border-radius: $radius-sm;
  letter-spacing: 0.5px;
}

.reply-body {
  font-size: $font-base;
  color: $text-regular;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-all;
}

// ====== 骨架 ======
.loading-wrap {
  padding-top: $space-md;
}
</style>
