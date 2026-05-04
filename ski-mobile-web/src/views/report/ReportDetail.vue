<template>
  <div class="report-page">
    <!-- 顶栏 -->
    <header class="nav-bar">
      <button class="back-btn" @click="router.back()">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
          <path d="M15 19L8 12l7-7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
      </button>
      <h1 class="nav-title">AI 教练报告</h1>
      <button class="share-btn" @click="handleShare">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
          <path d="M4 12v8a2 2 0 002 2h12a2 2 0 002-2v-8M16 6l-4-4-4 4M12 2v13"
                stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
      </button>
    </header>

    <div v-if="loading" class="loading-skeleton">
      <div class="skeleton" style="height: 180px; margin-bottom: 16px; border-radius: 16px;"></div>
      <div class="skeleton" style="height: 28px; width: 60%; margin-bottom: 12px;"></div>
      <div class="skeleton" style="height: 16px; margin-bottom: 8px;"></div>
      <div class="skeleton" style="height: 16px; margin-bottom: 8px;"></div>
      <div class="skeleton" style="height: 16px; width: 80%;"></div>
    </div>

    <div v-else-if="report" class="content">
      <!-- 报告头部 hero -->
      <div class="report-hero">
        <div class="hero-bg">
          <div class="orb orb-1"></div>
          <div class="orb orb-2"></div>
        </div>
        <div class="hero-content">
          <div class="hero-emoji">📄</div>
          <div class="hero-title">AI 教练点评</div>
          <div class="hero-meta">
            <span>{{ formatDateTime(report.createdTime) }}</span>
          </div>
        </div>
      </div>

      <!-- 报告正文 -->
      <article class="report-body">
        <MarkdownView :content="report.reportMarkdown" />
      </article>

      <!-- 底部操作 -->
      <div class="bottom-actions">
        <button class="action-btn" @click="router.push('/comparison/create')">
          <span class="emoji">📊</span>
          <span>与其他视频对比</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showSuccessToast, showFailToast } from 'vant'
import { getReport } from '@/api/report'
import { formatDateTime } from '@/utils/format'
import MarkdownView from '@/components/MarkdownView.vue'

const route = useRoute()
const router = useRouter()

const report = ref(null)
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    report.value = await getReport(route.params.id)
  } finally {
    loading.value = false
  }
})

async function handleShare() {
  // 简化版:复制当前页面链接
  try {
    await navigator.clipboard.writeText(window.location.href)
    showSuccessToast('链接已复制,可分享给朋友')
  } catch {
    showFailToast('当前浏览器不支持')
  }
}
</script>

<style lang="scss" scoped>
.report-page {
  min-height: 100vh;
  background: $bg-base;
  padding-bottom: 80px;
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
  padding: 0;
}

// ====== 报告 Hero ======
.report-hero {
  position: relative;
  padding: $space-3xl $space-lg;
  margin: $space-md $space-lg 0;
  border-radius: $radius-xl;
  background: linear-gradient(135deg, #DBEAFE 0%, #C7D2FE 50%, #FCE7F3 100%);
  overflow: hidden;
  color: $text-primary;
}

.hero-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  border-radius: $radius-xl;

  .orb {
    position: absolute;
    border-radius: 50%;
    filter: blur(40px);
  }
  .orb-1 {
    top: -40px; right: -40px;
    width: 160px; height: 160px;
    background: radial-gradient(circle, rgba(255,255,255,0.4) 0%, transparent 60%);
  }
  .orb-2 {
    bottom: -40px; left: -40px;
    width: 140px; height: 140px;
    background: radial-gradient(circle, rgba(244,114,182,0.4) 0%, transparent 60%);
  }
}

.hero-content {
  position: relative;
  z-index: 1;
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

.hero-meta {
  font-size: $font-sm;
  opacity: 0.85;
}

// ====== 报告正文 ======
.report-body {
  padding: $space-2xl $space-lg;
  animation: fadeIn 0.4s ease-out;
}

// ====== 底部操作 ======
.bottom-actions {
  padding: 0 $space-lg;
  margin-top: $space-md;
}

.action-btn {
  width: 100%;
  height: 52px;
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
  padding: $space-md $space-lg;
}
</style>
