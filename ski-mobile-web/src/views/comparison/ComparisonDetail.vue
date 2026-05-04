<template>
  <div class="page">
    <header class="nav-bar">
      <button class="back-btn" @click="router.back()">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
          <path d="M15 19L8 12l7-7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
      </button>
      <h1 class="nav-title">进步对比</h1>
      <button class="share-btn" @click="handleShare">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
          <path d="M4 12v8a2 2 0 002 2h12a2 2 0 002-2v-8M16 6l-4-4-4 4M12 2v13"
                stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
      </button>
    </header>

    <div v-if="loading" class="loading-skeleton">
      <div class="skeleton" style="height: 240px; margin: 16px; border-radius: 16px;"></div>
      <div class="skeleton" style="height: 100px; margin: 0 16px 16px; border-radius: 12px;"></div>
      <div class="skeleton" style="height: 200px; margin: 0 16px; border-radius: 12px;"></div>
    </div>

    <div v-else-if="report" class="content">
      <!-- Hero 区:进步统计 -->
      <div class="hero-card">
        <div class="hero-bg">
          <div class="orb orb-1"></div>
          <div class="orb orb-2"></div>
        </div>

        <div class="hero-content">
          <div class="hero-emoji">📊</div>
          <div class="hero-title">本次 vs 上次</div>
          <div class="hero-subtitle">
            {{ formatDate(report.createdTime) }}
          </div>

          <div class="metrics-grid">
            <div class="metric improved">
              <div class="metric-icon">📈</div>
              <div class="metric-num">{{ report.improvedCount || 0 }}</div>
              <div class="metric-label">进步项</div>
            </div>
            <div class="metric declined">
              <div class="metric-icon">📉</div>
              <div class="metric-num">{{ report.declinedCount || 0 }}</div>
              <div class="metric-label">退步项</div>
            </div>
            <div class="metric stable">
              <div class="metric-icon">🛡️</div>
              <div class="metric-num">{{ report.stabilityImprovedCount || 0 }}</div>
              <div class="metric-label">更稳定</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 视频对比卡片 -->
      <div v-if="prevVideo && currVideo" class="videos-row">
        <div class="video-card" @click="router.push(`/videos/${prevVideo.id}`)">
          <div class="video-tag">上次</div>
          <div class="video-thumb" :class="`thumb-${prevVideo.id % 5}`">
            <div class="thumb-icon">🎿</div>
          </div>
          <div class="video-info">
            <div class="video-name">{{ prevVideo.originalFilename }}</div>
            <div class="video-meta">{{ formatDate(prevVideo.createdTime) }}</div>
          </div>
        </div>

        <div class="vs-divider">
          <div class="vs-line"></div>
          <div class="vs-text">VS</div>
          <div class="vs-line"></div>
        </div>

        <div class="video-card" @click="router.push(`/videos/${currVideo.id}`)">
          <div class="video-tag tag-curr">本次</div>
          <div class="video-thumb" :class="`thumb-${currVideo.id % 5}`">
            <div class="thumb-icon">🎿</div>
          </div>
          <div class="video-info">
            <div class="video-name">{{ currVideo.originalFilename }}</div>
            <div class="video-meta">{{ formatDate(currVideo.createdTime) }}</div>
          </div>
        </div>
      </div>

      <!-- 完整教练点评 -->
      <div class="report-section">
        <div class="section-header">
          <div class="section-title">AI 教练完整点评</div>
          <div class="section-subtitle">基于姿态识别数据的逐项分析</div>
        </div>
        <div class="report-body">
          <MarkdownView :content="report.reportMarkdown" />
        </div>
      </div>

      <!-- 底部 CTA -->
      <div class="bottom-actions">
        <button class="action-btn" @click="router.push('/comparisons')">
          <span class="emoji">📜</span>
          查看所有对比报告
        </button>
        <button class="action-btn" @click="router.push('/comparison/create')">
          <span class="emoji">🆕</span>
          创建新的对比
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showSuccessToast, showFailToast } from 'vant'
import { getComparison } from '@/api/comparison'
import { getVideo } from '@/api/video'
import { formatDate } from '@/utils/format'
import MarkdownView from '@/components/MarkdownView.vue'

const route = useRoute()
const router = useRouter()

const report = ref(null)
const prevVideo = ref(null)
const currVideo = ref(null)
const loading = ref(true)

onMounted(async () => {
  loading.value = true
  try {
    const data = await getComparison(route.params.id)
    report.value = data

    const [prev, curr] = await Promise.all([
      getVideo(data.prevVideoId).catch(() => null),
      getVideo(data.currVideoId).catch(() => null)
    ])
    prevVideo.value = prev
    currVideo.value = curr
  } finally {
    loading.value = false
  }
})

async function handleShare() {
  try {
    await navigator.clipboard.writeText(window.location.href)
    showSuccessToast('链接已复制')
  } catch {
    showFailToast('当前浏览器不支持复制')
  }
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: $bg-base;
  padding-bottom: $space-2xl;
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
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid $border-light;

  .back-btn, .share-btn {
    width: 40px; height: 40px;
    display: flex; align-items: center; justify-content: center;
    color: $text-primary;
  }
  .nav-title {
    font-size: $font-md;
    font-weight: 600;
    color: $text-primary;
  }
}

.content { animation: fadeIn 0.4s ease-out; }

.hero-card {
  position: relative;
  margin: $space-md $space-lg 0;
  padding: $space-2xl $space-lg;
  border-radius: $radius-xl;
  background: linear-gradient(135deg, #DBEAFE 0%, #C7D2FE 50%, #FCE7F3 100%);
  overflow: hidden;
  color: $text-primary;
}

.hero-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  border-radius: $radius-xl;
  overflow: hidden;

  .orb {
    position: absolute;
    border-radius: 50%;
    filter: blur(40px);
  }
  .orb-1 {
    top: -50px; right: -30px;
    width: 180px; height: 180px;
    background: radial-gradient(circle, rgba(255,255,255,0.4), transparent 60%);
  }
  .orb-2 {
    bottom: -40px; left: -40px;
    width: 160px; height: 160px;
    background: radial-gradient(circle, rgba(244,114,182,0.5), transparent 60%);
  }
}

.hero-content {
  position: relative;
  z-index: 1;
  text-align: center;
}

.hero-emoji {
  font-size: 40px;
  margin-bottom: $space-sm;
  filter: drop-shadow(0 4px 12px rgba(0,0,0,0.2));
}

.hero-title {
  font-size: $font-3xl;
  font-weight: 700;
  margin-bottom: 4px;
  letter-spacing: -0.5px;
}

.hero-subtitle {
  font-size: $font-sm;
  opacity: 0.85;
  margin-bottom: $space-lg;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: $space-sm;
}

.metric {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: $radius-md;
  padding: $space-md $space-xs;
  text-align: center;

  .metric-icon { font-size: 22px; margin-bottom: 4px; }
  .metric-num {
    font-size: 28px;
    font-weight: 700;
    line-height: 1;
    margin-bottom: 4px;
  }
  .metric-label {
    font-size: $font-xs;
    opacity: 0.85;
  }

  &.improved .metric-num { color: #4ade80; }
  &.declined .metric-num { color: #fca5a5; }
  &.stable .metric-num { color: #67e8f9; }
}

.videos-row {
  margin: $space-lg $space-lg 0;
  background: $bg-card;
  border: 1px solid $border-light;
  border-radius: $radius-lg;
  padding: $space-md;
  display: flex;
  align-items: stretch;
  gap: $space-sm;
}

.video-card {
  flex: 1;
  text-align: center;
  position: relative;
  display: flex;
  flex-direction: column;
  cursor: pointer;
  border-radius: $radius-md;
  padding: $space-sm;
  background: $bg-elevated;
  border: 1.5px solid $border-light;
  transition: all 0.15s;

  &:active {
    transform: scale(0.96);
    border-color: $color-primary;
    box-shadow: 0 0 0 3px rgba(14, 143, 212, 0.15);
  }
}

.video-tag {
  display: inline-block;
  padding: 2px 8px;
  background: rgba(6, 182, 212, 0.15);
  color: $color-cyan;
  border-radius: $radius-sm;
  font-size: 10px;
  font-weight: 600;
  margin-bottom: $space-sm;
  align-self: center;

  &.tag-curr {
    background: rgba(139, 92, 246, 0.15);
    color: $color-purple;
  }
}

.video-thumb {
  width: 100%;
  aspect-ratio: 16/9;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: $space-sm;

  &.thumb-0 { background: linear-gradient(135deg, #1e3a8a, #6d28d9); }
  &.thumb-1 { background: linear-gradient(135deg, #075985, #0c4a6e); }
  &.thumb-2 { background: linear-gradient(135deg, #4c1d95, #7e22ce); }
  &.thumb-3 { background: linear-gradient(135deg, #134e4a, #0f766e); }
  &.thumb-4 { background: linear-gradient(135deg, #831843, #be185d); }
}

.thumb-icon { font-size: 32px; }

.video-info { text-align: left; padding: 0 4px; }

.video-name {
  font-size: $font-xs;
  color: $text-primary;
  font-weight: 500;
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.video-meta {
  font-size: 10px;
  color: $text-secondary;
  margin-bottom: $space-sm;
}

.vs-divider {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 28px;
  flex-shrink: 0;

  .vs-line {
    flex: 1;
    width: 1px;
    background: $border-light;
  }
  .vs-text {
    font-size: 11px;
    font-weight: 700;
    color: $color-purple;
    padding: 4px 0;
    letter-spacing: 0.5px;
  }
}

.report-section {
  margin: $space-lg $space-lg 0;
}

.section-header {
  margin-bottom: $space-md;
  border-left: 3px solid $color-primary;
  padding-left: $space-md;

  .section-title {
    font-size: $font-xl;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: 2px;
  }
  .section-subtitle {
    font-size: $font-xs;
    color: $text-secondary;
  }
}

.report-body {
  background: $bg-card;
  border: 1px solid $border-light;
  border-radius: $radius-lg;
  padding: $space-lg;
}

.bottom-actions {
  padding: $space-lg;
  display: flex;
  flex-direction: column;
  gap: $space-md;
}

.action-btn {
  width: 100%;
  height: 48px;
  background: $bg-card;
  border: 1px solid $border-base;
  border-radius: $radius-md;
  color: $text-primary;
  font-size: $font-md;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $space-sm;

  &:active { transform: scale(0.98); }

  .emoji { font-size: 18px; }
}

.loading-skeleton {
  padding-top: $space-md;
}
</style>
