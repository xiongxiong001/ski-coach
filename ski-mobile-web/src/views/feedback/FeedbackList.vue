<template>
  <div class="page">
    <header class="nav-bar">
      <button class="back-btn" @click="router.back()">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
          <path d="M15 19L8 12l7-7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
      </button>
      <h1 class="nav-title">我的反馈</h1>
      <div class="nav-spacer"></div>
    </header>

    <!-- 统计 banner -->
    <div class="stats-banner">
      <div class="stats-bg">
        <div class="orb orb-1"></div>
        <div class="orb orb-2"></div>
      </div>
      <div class="stats-content">
        <div class="stats-summary">
          <span class="summary-num">{{ stats.totalCount ?? 0 }}</span>条反馈，感谢你让产品变得更好
        </div>
        <div class="stats-row">
          <div class="stat-item">
            <div class="stat-num">{{ stats.pendingCount ?? '-' }}</div>
            <div class="stat-label">待处理</div>
          </div>
          <div class="stat-item">
            <div class="stat-num">{{ stats.viewedCount ?? '-' }}</div>
            <div class="stat-label">已查看</div>
          </div>
          <div class="stat-item">
            <div class="stat-num">{{ stats.repliedCount ?? '-' }}</div>
            <div class="stat-label">已回复</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 筛选 chips -->
    <div class="filter-row">
      <span
        v-for="f in filters"
        :key="f.value"
        class="chip"
        :class="{ active: activeFilter === f.value }"
        @click="onFilter(f.value)"
      >{{ f.label }}</span>
    </div>

    <!-- 加载骨架 -->
    <div v-if="loading && !list.length" class="loading-wrap">
      <div v-for="i in 3" :key="i" class="skeleton" style="height: 100px; margin: 0 16px 12px; border-radius: 12px;"></div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!loading && !list.length" class="empty">
      <div class="empty-emoji">📝</div>
      <div class="empty-text">还没有反馈记录</div>
      <button class="empty-btn" @click="router.push('/feedback')">去提交反馈</button>
    </div>

    <!-- 反馈卡片列表 -->
    <div v-else class="list">
      <div
        v-for="item in list"
        :key="item.id"
        class="card"
        :class="`stripe-${item.type}`"
        @click="router.push(`/feedback/${item.id}`)"
      >
        <div class="card-header">
          <div class="card-meta">
            <span class="card-type-tag" :class="`tag-${item.type}`">
              {{ typeMap[item.type]?.label || item.type }}
            </span>
            <span class="card-status-tag" :class="`status-${item.status}`">
              {{ statusMap[item.status] }}
            </span>
          </div>
          <span class="card-time">{{ item.createdTime }}</span>
        </div>

        <div class="card-content">{{ item.content }}</div>

        <!-- 官方回复 -->
        <div v-if="item.status === 2 && item.reply" class="reply-box">
          <div class="reply-label">官方回复</div>
          <div class="reply-text">{{ item.reply }}</div>
        </div>

        <div class="card-footer">
          <span v-if="item.imageCount" class="card-images">📷 {{ item.imageCount }}张截图</span>
        </div>
      </div>

      <div v-if="hasMore" class="load-more" @click="loadMore">加载更多</div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listFeedbacks, getFeedbackStats } from '@/api/feedback'

const router = useRouter()

const typeMap = {
  bug: { label: 'BUG反馈' },
  feature: { label: '功能建议' },
  performance: { label: '测速反馈' },
  other: { label: '其他' }
}

const statusMap = ['待处理', '已查看', '已回复']

const filters = [
  { value: '', label: '全部' },
  { value: 'bug', label: 'BUG' },
  { value: 'feature', label: '功能建议' },
  { value: 'performance', label: '测速' },
  { value: 'other', label: '其他' }
]

const stats = reactive({ totalCount: 0, pendingCount: 0, viewedCount: 0, repliedCount: 0 })
const activeFilter = ref('')
const list = ref([])
const loading = ref(true)
const pageNum = ref(1)
const total = ref(0)
const hasMore = ref(false)

onMounted(async () => {
  await Promise.all([loadStats(), loadMore()])
})

async function loadStats() {
  try {
    const data = await getFeedbackStats()
    Object.assign(stats, data)
  } catch { /* ignore */ }
}

async function loadMore() {
  loading.value = !list.value.length
  try {
    const data = await listFeedbacks({
      pageNum: pageNum.value,
      pageSize: 20,
      type: activeFilter.value || undefined
    })
    list.value.push(...(data.records || []))
    total.value = data.total || 0
    hasMore.value = list.value.length < total.value
    pageNum.value++
  } finally {
    loading.value = false
  }
}

function onFilter(value) {
  if (activeFilter.value === value) return
  activeFilter.value = value
  list.value = []
  pageNum.value = 1
  total.value = 0
  hasMore.value = false
  loadMore()
}
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

// ====== 统计 banner ======
.stats-banner {
  position: relative;
  margin: $space-md $space-lg;
  padding: $space-2xl $space-lg;
  border-radius: $radius-xl;
  background: linear-gradient(135deg, #1E40AF 0%, #6366F1 50%, #8B5CF6 100%);
  overflow: hidden;
}

.stats-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  border-radius: $radius-xl;
  overflow: hidden;

  .orb {
    position: absolute;
    border-radius: 50%;
    filter: blur(50px);
  }
  .orb-1 {
    top: -40px; right: -30px;
    width: 140px; height: 140px;
    background: radial-gradient(circle, rgba(255,255,255,0.15) 0%, transparent 60%);
  }
  .orb-2 {
    bottom: -40px; left: -40px;
    width: 120px; height: 120px;
    background: radial-gradient(circle, rgba(167,139,250,0.3) 0%, transparent 60%);
  }
}

.stats-content {
  position: relative;
  z-index: 1;
}

.stats-summary {
  text-align: center;
  margin-bottom: $space-xl;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.75);
  letter-spacing: 0.5px;

  .summary-num {
    font-size: 52px;
    font-weight: 700;
    color: white;
    vertical-align: baseline;
    margin-right: 8px;
  }
}

.stats-row {
  display: flex;
  justify-content: space-around;
}

.stat-item {
  text-align: center;
}

.stat-num {
  font-size: 32px;
  font-weight: 700;
  color: white;
  line-height: 1;
  margin-bottom: 4px;
  letter-spacing: -0.5px;
}

.stat-label {
  font-size: $font-xs;
  color: rgba(255, 255, 255, 0.7);
}

// ====== 筛选 chips ======
.filter-row {
  display: flex;
  gap: $space-sm;
  padding: $space-md $space-lg;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;

  &::-webkit-scrollbar { display: none; }
}

.chip {
  flex-shrink: 0;
  padding: 6px 14px;
  border-radius: $radius-full;
  font-size: $font-xs;
  font-weight: 500;
  color: $text-secondary;
  background: $bg-card;
  border: 1px solid $border-light;
  cursor: pointer;
  transition: all 0.15s;

  &.active {
    color: $color-primary;
    background: rgba(37, 99, 235, 0.08);
    border-color: transparent;
  }
}

// ====== 列表 ======
.list {
  padding: 0 $space-lg;
}

.card {
  background: $bg-card;
  border: 1px solid $border-light;
  border-radius: $radius-lg;
  padding: $space-md $space-lg;
  margin-bottom: $space-md;
  position: relative;
  overflow: hidden;

  // 左侧色条
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
  margin-bottom: $space-sm;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: $space-sm;
}

.card-type-tag {
  font-size: $font-xs;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: $radius-sm;

  &.tag-bug          { background: rgba(239, 68, 68, 0.08); color: #DC2626; }
  &.tag-feature      { background: rgba(37, 99, 235, 0.08); color: #2563EB; }
  &.tag-performance  { background: rgba(245, 158, 11, 0.08); color: #D97706; }
  &.tag-other        { background: $bg-elevated; color: $text-secondary; }
}

.card-status-tag {
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

.card-content {
  font-size: $font-sm;
  color: $text-primary;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: $space-sm;
}

// ====== 官方回复 ======
.reply-box {
  margin: $space-sm 0;
  padding: $space-md;
  background: rgba(16, 185, 129, 0.05);
  border-radius: $radius-md;
  border-left: 3px solid $color-success;
}

.reply-label {
  font-size: 10px;
  font-weight: 600;
  color: $color-success;
  margin-bottom: 4px;
  letter-spacing: 0.5px;
}

.reply-text {
  font-size: $font-xs;
  color: $text-regular;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

// ====== 底部 ======
.card-footer {
  display: flex;
  align-items: center;
}

.card-images {
  font-size: $font-xs;
  color: $text-secondary;
}

.load-more {
  text-align: center;
  padding: $space-md;
  color: $color-primary;
  font-size: $font-sm;
  cursor: pointer;
}

// ====== 空状态 ======
.empty {
  text-align: center;
  padding: $space-4xl 0;

  .empty-emoji { font-size: 48px; margin-bottom: $space-md; opacity: 0.6; }
  .empty-text { color: $text-secondary; margin-bottom: $space-lg; font-size: $font-base; }
  .empty-btn {
    padding: 10px 24px;
    background: $gradient-primary;
    color: white;
    border-radius: $radius-full;
    font-size: $font-sm;
    font-weight: 500;
    cursor: pointer;
    box-shadow: $shadow-glow;
  }
}

.loading-wrap {
  padding-top: $space-md;
}
</style>
