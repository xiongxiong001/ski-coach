<template>
  <div class="page">
    <header class="page-header">
      <h2>对比报告</h2>
      <p class="header-sub">{{ total ? `${total} 份对比报告` : '看到自己的进步' }}</p>
    </header>

    <div class="content">
      <div v-if="loading && !list.length" class="skeleton-list">
        <div v-for="i in 3" :key="i" class="card-skeleton skeleton"></div>
      </div>

      <div v-else-if="!list.length" class="empty">
        <div class="empty-illustration">
          <div class="bar bar-1"></div>
          <div class="bar bar-2"></div>
          <div class="bar bar-3"></div>
          <div class="bar bar-4"></div>
        </div>
        <div class="empty-title">还没有对比报告</div>
        <div class="empty-desc">至少需要 2 个分析完成的视频</div>
        <button class="empty-btn" @click="router.push('/comparison/create')">
          创建第一份对比
        </button>
      </div>

      <van-list
        v-else
        v-model:loading="loadingMore"
        :finished="finished"
        :immediate-check="false"
        finished-text="没有更多了"
        @load="loadMore"
      >
        <div
          v-for="r in list"
          :key="r.id"
          class="comparison-card"
          @click="router.push(`/comparisons/${r.id}`)"
        >
          <!-- 顶部:渐变 hero 缩略 -->
          <div class="card-hero">
            <div class="hero-decor"></div>
            <div class="hero-emoji">📊</div>
            <div class="hero-text">
              <div class="hero-title">本次 vs 上次</div>
              <div class="hero-time">{{ formatDate(r.createdTime) }}</div>
            </div>
          </div>

          <!-- 三个统计指标 -->
          <div class="card-metrics">
            <div class="metric improved">
              <span class="metric-icon">📈</span>
              <span class="metric-num">{{ r.improvedCount || 0 }}</span>
              <span class="metric-label">进步</span>
            </div>
            <div class="metric declined">
              <span class="metric-icon">📉</span>
              <span class="metric-num">{{ r.declinedCount || 0 }}</span>
              <span class="metric-label">退步</span>
            </div>
            <div class="metric stable">
              <span class="metric-icon">🛡️</span>
              <span class="metric-num">{{ r.stabilityImprovedCount || 0 }}</span>
              <span class="metric-label">更稳</span>
            </div>
          </div>
        </div>
      </van-list>
    </div>

    <!-- 浮动创建按钮 -->
    <div v-if="list.length" class="fab" @click="router.push('/comparison/create')">
      <svg width="28" height="28" viewBox="0 0 24 24" fill="none">
        <path d="M12 5v14M5 12h14" stroke="white" stroke-width="2.5" stroke-linecap="round" />
      </svg>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listComparisons } from '@/api/comparison'
import { formatDate } from '@/utils/format'

const router = useRouter()

const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const loadingMore = ref(false)
const finished = ref(false)

onMounted(() => loadList(true))

async function loadList(reset = false) {
  if (reset) {
    pageNum.value = 1
    finished.value = false
    list.value = []
  }
  loading.value = true
  try {
    const data = await listComparisons({
      pageNum: pageNum.value,
      pageSize: pageSize.value
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
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: $bg-base;
  position: relative;
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

.content {
  padding: 0 $space-lg;
}

// ====== 对比卡片 ======
.comparison-card {
  background: $bg-card;
  border: 1px solid $border-light;
  border-radius: $radius-lg;
  margin-bottom: $space-md;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.15s;

  &:active { transform: scale(0.99); }
}

.card-hero {
  position: relative;
  padding: $space-md $space-lg;
  background: linear-gradient(135deg, #1e3a8a 0%, #6d28d9 50%, #be185d 100%);
  display: flex;
  align-items: center;
  gap: $space-md;
  color: white;
  overflow: hidden;
}

.hero-decor {
  position: absolute;
  top: -30px; right: -20px;
  width: 120px; height: 120px;
  background: radial-gradient(circle, rgba(255,255,255,0.2), transparent 60%);
  border-radius: 50%;
  filter: blur(20px);
}

.hero-emoji {
  font-size: 32px;
  flex-shrink: 0;
  filter: drop-shadow(0 4px 8px rgba(0,0,0,0.2));
  position: relative;
  z-index: 1;
}

.hero-text {
  flex: 1;
  position: relative;
  z-index: 1;
}

.hero-title {
  font-size: $font-md;
  font-weight: 600;
}

.hero-time {
  font-size: $font-xs;
  opacity: 0.85;
  margin-top: 2px;
}

.card-metrics {
  display: flex;
  padding: $space-md $space-lg;
  gap: $space-md;
}

.metric {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: $space-sm $space-md;
  background: $bg-elevated;
  border-radius: $radius-md;

  .metric-icon { font-size: 16px; }
  .metric-num {
    font-size: $font-lg;
    font-weight: 700;
    margin-left: 2px;
  }
  .metric-label {
    font-size: $font-xs;
    color: $text-secondary;
    margin-left: auto;
  }

  &.improved .metric-num { color: $color-success; }
  &.declined .metric-num { color: $color-danger; }
  &.stable .metric-num { color: $color-cyan; }
}

// ====== 加载骨架 ======
.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: $space-md;
}

.card-skeleton {
  height: 160px;
  border-radius: $radius-lg;
}

// ====== 空状态 ======
.empty {
  text-align: center;
  padding: $space-3xl 0;
}

.empty-illustration {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 8px;
  height: 120px;
  margin-bottom: $space-2xl;

  .bar {
    width: 24px;
    border-radius: 6px 6px 0 0;
    animation: bar-grow 2s ease infinite;
  }
  .bar-1 { height: 40%; background: linear-gradient(180deg, #06B6D4, #3B82F6); animation-delay: 0s; }
  .bar-2 { height: 70%; background: linear-gradient(180deg, #3B82F6, #8B5CF6); animation-delay: 0.2s; }
  .bar-3 { height: 55%; background: linear-gradient(180deg, #8B5CF6, #F472B6); animation-delay: 0.4s; }
  .bar-4 { height: 85%; background: linear-gradient(180deg, #F472B6, #EF4444); animation-delay: 0.6s; }
}

@keyframes bar-grow {
  0%, 100% { transform: scaleY(0.6); transform-origin: bottom; }
  50% { transform: scaleY(1); transform-origin: bottom; }
}

.empty-title {
  font-size: $font-lg;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: $space-sm;
}

.empty-desc {
  font-size: $font-sm;
  color: $text-secondary;
  margin-bottom: $space-2xl;
}

.empty-btn {
  padding: 12px 28px;
  background: $gradient-primary;
  color: white;
  border-radius: $radius-full;
  font-size: $font-md;
  font-weight: 500;
  cursor: pointer;
  box-shadow: $shadow-glow;

  &:active { transform: scale(0.96); }
}

// ====== 浮动创建按钮 ======
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
}
</style>
