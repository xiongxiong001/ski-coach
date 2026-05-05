<template>
  <div class="home-page">
    <!-- 顶部:背景渐变 + 个性化问候 -->
    <header class="hero">
      <div class="hero-bg">
        <div class="orb orb-1"></div>
        <div class="orb orb-2"></div>
      </div>
      <div class="hero-content">
        <div class="greeting-row">
          <div>
            <div class="greeting">{{ greeting }}</div>
            <div class="username">{{ userStore.nickname }} 👋</div>
          </div>
          <div class="avatar" @click="router.push('/profile')">
            {{ avatarText }}
          </div>
        </div>

        <!-- 大CTA卡片 -->
        <div class="cta-card" @click="router.push('/videos')">
          <div class="cta-content">
            <div class="cta-emoji">⛷️</div>
            <div class="cta-text">
              <div class="cta-title">上传滑雪视频</div>
              <div class="cta-subtitle">AI 教练为你逐项分析</div>
            </div>
          </div>
          <div class="cta-arrow">→</div>
        </div>
      </div>
    </header>

    <!-- 统计 -->
    <section class="stats-section">
      <div class="stat-card" @click="router.push('/videos')">
        <div class="stat-icon">🎬</div>
        <div class="stat-num">{{ stats.totalVideos }}</div>
        <div class="stat-label">总视频</div>
      </div>
      <div class="stat-card" @click="router.push('/videos')">
        <div class="stat-icon">📝</div>
        <div class="stat-num">{{ stats.totalReports }}</div>
        <div class="stat-label">收到报告</div>
      </div>
      <div class="stat-card" @click="router.push('/comparisons')">
        <div class="stat-icon">📊</div>
        <div class="stat-num">{{ stats.totalComparisons }}</div>
        <div class="stat-label">对比报告</div>
      </div>
    </section>

    <!-- 最近报告 -->
    <section class="section">
      <div class="section-header">
        <h3>最近的报告</h3>
        <span class="more-link" @click="router.push('/videos')">查看全部 →</span>
      </div>

      <!-- 加载骨架 -->
      <div v-if="loading" class="reports-list">
        <div v-for="i in 2" :key="i" class="report-skeleton skeleton"></div>
      </div>

      <!-- 空状态 -->
      <div v-else-if="!recentReports.length" class="empty-state">
        <div class="empty-emoji">🎿</div>
        <div class="empty-text">还没有报告</div>
        <button class="empty-btn" @click="router.push('/videos')">上传第一个视频</button>
      </div>

      <!-- 报告列表 -->
      <div v-else class="reports-list">
        <div
          v-for="r in recentReports"
          :key="r.id"
          class="report-card"
          @click="router.push(`/reports/${r.id}`)"
        >
          <!-- 视频缩略图占位 -->
          <div class="report-thumb" :class="`thumb-${r.videoId % 5}`">
            <div class="thumb-icon">⛷️</div>
            <div v-if="r.videoDurationSeconds" class="duration-badge">
              {{ formatDuration(r.videoDurationSeconds) }}
            </div>
          </div>
          <div class="report-info">
            <div class="report-title">{{ r.videoFilename || 'AI 教练点评' }}</div>
            <div class="report-time">{{ formatDate(r.createdTime) }}</div>
          </div>
          <div class="report-arrow">›</div>
        </div>
      </div>
    </section>

    <!-- 拍摄技巧 -->
    <section class="section tips-section">
      <div class="tips-card">
        <div class="tips-header">
          <span class="tips-emoji">📌</span>
          <span class="tips-title">怎么拍才能拿到好的分析?</span>
        </div>
        <ul class="tips-list">
          <li><span class="tip-key">视角</span>第三人称(让朋友帮拍),全身入镜</li>
          <li><span class="tip-key">时长</span>40 秒到 1 分钟,不超过 50MB</li>
          <li><span class="tip-key">动作</span>包含完整转弯(至少 2-3 个连续转弯)</li>
          <li><span class="tip-key">格式</span>支持 mp4、mov、m4v</li>
        </ul>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { listVideos } from '@/api/video'
import { listReports } from '@/api/report'
import { listComparisons } from '@/api/comparison'
import { formatDate, formatDuration } from '@/utils/format'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const recentReports = ref([])
const stats = ref({ totalVideos: 0, totalReports: 0, totalComparisons: 0 })

// 根据时间显示问候
const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深啦'
  if (h < 11) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const avatarText = computed(() => {
  const name = userStore.nickname
  return name?.charAt(0) || '雪'
})

onMounted(async () => {
  try {
    const [videoData, reportData, comparisonData] = await Promise.all([
      listVideos({ pageNum: 1, pageSize: 1 }),
      listReports({ pageNum: 1, pageSize: 3 }),
      listComparisons({ pageNum: 1, pageSize: 1 }).catch(() => ({ total: 0, records: [] }))
    ])
    stats.value.totalVideos = videoData.total || 0
    stats.value.totalReports = reportData.total || 0
    stats.value.totalComparisons = comparisonData.total || 0
    recentReports.value = reportData.records || []
  } catch (e) {
    // 错误已在http拦截器toast
  } finally {
    loading.value = false
  }
})
</script>

<style lang="scss" scoped>
.home-page {
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
}

.greeting-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $space-2xl;

  .greeting {
    font-size: $font-base;
    color: $text-secondary;
    margin-bottom: 4px;
  }
  .username {
    font-size: $font-3xl;
    font-weight: 700;
    color: $text-primary;
    letter-spacing: -0.5px;
  }

  .avatar {
    width: 44px;
    height: 44px;
    border-radius: 50%;
    background: $gradient-primary;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    font-weight: 600;
    font-size: $font-lg;
    cursor: pointer;
    box-shadow: $shadow-glow;
  }
}

// ====== 大CTA ======
.cta-card {
  background: $gradient-primary;
  border-radius: $radius-xl;
  padding: $space-xl;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: white;
  cursor: pointer;
  box-shadow: $shadow-glow, 0 8px 24px rgba(59,130,246,0.3);
  transition: transform 0.15s;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: -40%; right: -20%;
    width: 220px; height: 220px;
    background: radial-gradient(circle, rgba(255,255,255,0.15), transparent);
    border-radius: 50%;
  }

  &:active { transform: scale(0.98); }

  .cta-content {
    display: flex;
    align-items: center;
    gap: $space-md;
    position: relative;
    z-index: 1;
  }
  .cta-emoji {
    font-size: 36px;
  }
  .cta-title {
    font-size: $font-xl;
    font-weight: 600;
    margin-bottom: 2px;
    letter-spacing: -0.3px;
  }
  .cta-subtitle {
    font-size: $font-sm;
    opacity: 0.85;
  }
  .cta-arrow {
    font-size: 24px;
    font-weight: 300;
    position: relative;
    z-index: 1;
  }
}

// ====== 统计 ======
.stats-section {
  padding: 0 $space-lg;
  margin-top: -$space-2xl;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: $space-md;
  position: relative;
  z-index: 2;
}

.stat-card {
  background: $bg-card;
  border: 1px solid $border-light;
  border-radius: $radius-lg;
  padding: $space-md;
  text-align: center;
  cursor: pointer;
  transition: all 0.15s;

  &:active { transform: scale(0.96); }

  .stat-icon {
    font-size: 22px;
    margin-bottom: 4px;
  }
  .stat-num {
    font-size: 24px;
    font-weight: 700;
    background: $gradient-primary;
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
    line-height: 1.1;
  }
  .stat-label {
    font-size: $font-xs;
    color: $text-secondary;
    margin-top: 2px;
  }
}

// ====== 通用 section ======
.section {
  padding: $space-2xl $space-lg 0;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $space-md;

  h3 {
    font-size: $font-xl;
    font-weight: 600;
    color: $text-primary;
    letter-spacing: -0.3px;
  }
  .more-link {
    color: $color-primary;
    font-size: $font-sm;
    cursor: pointer;
  }
}

// ====== 报告列表 ======
.reports-list {
  display: flex;
  flex-direction: column;
  gap: $space-md;
}

.report-card {
  background: $bg-card;
  border: 1px solid $border-light;
  border-radius: $radius-lg;
  padding: $space-md;
  display: flex;
  align-items: center;
  gap: $space-md;
  cursor: pointer;
  transition: all 0.15s;

  &:active { transform: scale(0.98); border-color: $color-primary; }

  // 视频缩略图占位
  .report-thumb {
    width: 44px;
    height: 44px;
    border-radius: $radius-md;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    position: relative;
    overflow: hidden;

    &.thumb-0 { background: linear-gradient(135deg, #1e3a8a 0%, #6d28d9 100%); }
    &.thumb-1 { background: linear-gradient(135deg, #075985 0%, #0c4a6e 100%); }
    &.thumb-2 { background: linear-gradient(135deg, #4c1d95 0%, #7e22ce 100%); }
    &.thumb-3 { background: linear-gradient(135deg, #134e4a 0%, #0f766e 100%); }
    &.thumb-4 { background: linear-gradient(135deg, #831843 0%, #be185d 100%); }

    &::after {
      content: '';
      position: absolute;
      inset: 0;
      background: radial-gradient(circle at 30% 30%, rgba(255,255,255,0.12) 0%, transparent 60%);
    }
  }
  .thumb-icon {
    font-size: 18px;
    position: relative;
    z-index: 1;
  }
  .duration-badge {
    position: absolute;
    bottom: 2px;
    right: 2px;
    font-size: 9px;
    color: white;
    background: rgba(0,0,0,0.6);
    padding: 1px 4px;
    border-radius: 2px;
    z-index: 1;
  }
  .report-info {
    flex: 1;
    min-width: 0;
  }
  .report-title {
    font-size: $font-md;
    font-weight: 500;
    color: $text-primary;
    margin-bottom: 2px;
  }
  .report-time {
    font-size: $font-xs;
    color: $text-secondary;
  }
  .report-arrow {
    font-size: 22px;
    color: $text-secondary;
    line-height: 1;
  }
}

.report-skeleton {
  height: 68px;
  border-radius: $radius-lg;
}

.empty-state {
  text-align: center;
  padding: $space-3xl 0;

  .empty-emoji {
    font-size: 48px;
    margin-bottom: $space-md;
    opacity: 0.6;
  }
  .empty-text {
    color: $text-secondary;
    margin-bottom: $space-lg;
    font-size: $font-base;
  }
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

// ====== 提示卡片 ======
.tips-section {
  padding-top: $space-2xl;
  padding-bottom: $space-2xl;
}

.tips-card {
  background: $glass-bg;
  border: $glass-border;
  border-radius: $radius-lg;
  padding: $space-lg;
  backdrop-filter: $glass-blur;
}

.tips-header {
  display: flex;
  align-items: center;
  gap: $space-sm;
  margin-bottom: $space-md;
  font-weight: 600;
  font-size: $font-md;
  color: $text-primary;

  .tips-emoji { font-size: 18px; }
}

.tips-list {
  list-style: none;
  padding: 0;

  li {
    padding: $space-sm 0;
    font-size: $font-sm;
    color: $text-regular;
    line-height: 1.7;
    border-top: 1px solid $border-light;

    &:first-child { border-top: none; }
  }

  .tip-key {
    display: inline-block;
    color: $color-cyan;
    font-weight: 500;
    margin-right: $space-sm;
    min-width: 36px;
  }
}
</style>