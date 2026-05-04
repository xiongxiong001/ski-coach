<template>
  <div class="page">
    <div class="bg-decoration">
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
      <div class="orb orb-3"></div>
    </div>

    <div class="content">
      <!-- 状态图标 -->
      <div class="status-icon" :class="statusClass">
        <div v-if="status === 'failed'" class="failed-icon">😢</div>
        <div v-else class="ai-rings">
          <div class="ring r1"></div>
          <div class="ring r2"></div>
          <div class="ring r3"></div>
          <div class="ai-emoji">📊</div>
        </div>
      </div>

      <!-- 文字 -->
      <div class="status-title">{{ statusTitle }}</div>
      <div class="status-subtitle">{{ statusSubtitle }}</div>

      <!-- 用时 -->
      <div v-if="status !== 'failed'" class="elapsed">{{ elapsedText }}</div>

      <!-- 进度dots -->
      <div v-if="status === 'pending' || status === 'running'" class="dots">
        <div class="dot"></div>
        <div class="dot"></div>
        <div class="dot"></div>
      </div>

      <!-- 失败时按钮 -->
      <div v-if="status === 'failed'" class="actions">
        <div class="error-msg">{{ errorMsg }}</div>
        <button class="primary-btn" @click="router.replace('/comparison/create')">
          返回重试
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getTaskStatus } from '@/api/task'
import { TASK_POLL_INTERVAL } from '@/utils/constants'

const router = useRouter()
const route = useRoute()

const taskId = computed(() => route.query.taskId)
const status = ref('pending')
const errorMsg = ref('')
const elapsed = ref(0)
const pollTimer = ref(null)
const elapsedTimer = ref(null)

const statusClass = computed(() => `status-${status.value}`)

const statusTitle = computed(() => {
  switch (status.value) {
    case 'pending': return 'AI 排队中'
    case 'running': return 'AI 正在对比分析'
    case 'success': return '分析完成!'
    case 'failed':  return '生成失败'
    default: return '加载中'
  }
})

const statusSubtitle = computed(() => {
  switch (status.value) {
    case 'pending': return '前面还有任务,马上轮到你'
    case 'running': return '通常 10-30 秒完成,正在和上次的视频对比'
    case 'success': return '正在跳转到对比报告...'
    case 'failed':  return '请稍后再试'
    default: return ''
  }
})

const elapsedText = computed(() => {
  return `已用时 ${elapsed.value}s`
})

onMounted(() => {
  if (!taskId.value) {
    router.replace('/comparisons')
    return
  }

  // 立刻拉一次
  fetchStatus()
  // 然后每 3s 轮询
  pollTimer.value = setInterval(fetchStatus, TASK_POLL_INTERVAL)
  // 计时
  elapsedTimer.value = setInterval(() => { elapsed.value++ }, 1000)
})

onUnmounted(() => {
  if (pollTimer.value) clearInterval(pollTimer.value)
  if (elapsedTimer.value) clearInterval(elapsedTimer.value)
})

async function fetchStatus() {
  try {
    const data = await getTaskStatus(taskId.value)
    status.value = data.status

    if (data.status === 'success') {
      stopPolling()
      // 跳转到对比报告详情
      setTimeout(() => {
        router.replace(`/comparisons/${data.reportId}`)
      }, 800)
    } else if (data.status === 'failed') {
      stopPolling()
      errorMsg.value = data.errorMessage || '未知错误'
    }
  } catch (e) {
    // 错误已在拦截器里 toast,继续轮询
  }
}

function stopPolling() {
  if (pollTimer.value) {
    clearInterval(pollTimer.value)
    pollTimer.value = null
  }
  if (elapsedTimer.value) {
    clearInterval(elapsedTimer.value)
    elapsedTimer.value = null
  }
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: $bg-base;
  position: relative;
  overflow: hidden;
}

.bg-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;

  .orb {
    position: absolute;
    border-radius: 50%;
    filter: blur(60px);
    animation: float 8s ease-in-out infinite;
  }
  .orb-1 {
    top: 10%; right: -60px;
    width: 240px; height: 240px;
    background: radial-gradient(circle, rgba(14,165,233,0.4) 0%, transparent 70%);
    animation-delay: 0s;
  }
  .orb-2 {
    bottom: 20%; left: -80px;
    width: 280px; height: 280px;
    background: radial-gradient(circle, rgba(56,189,248,0.4) 0%, transparent 70%);
    animation-delay: 2s;
  }
  .orb-3 {
    top: 50%; left: 50%; transform: translate(-50%, -50%);
    width: 200px; height: 200px;
    background: radial-gradient(circle, rgba(8,145,178,0.3) 0%, transparent 70%);
    animation-delay: 4s;
  }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-20px); }
}

.content {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  padding: 0 $space-2xl;
  padding-top: calc(80px + #{$safe-top});
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.status-icon {
  margin-bottom: $space-2xl;
  position: relative;
  width: 140px;
  height: 140px;
  display: flex;
  align-items: center;
  justify-content: center;

  &.status-success .ai-emoji { transform: scale(1.2); }
}

.ai-rings {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;

  .ring {
    position: absolute;
    inset: 0;
    border: 2px solid $color-primary;
    border-radius: 50%;
    opacity: 0;
    animation: ring-pulse 2.5s infinite;
  }
  .r1 { animation-delay: 0s; }
  .r2 { animation-delay: 0.83s; border-color: $color-purple; }
  .r3 { animation-delay: 1.66s; border-color: $color-cyan; }

  .ai-emoji {
    font-size: 64px;
    z-index: 2;
    transition: transform 0.5s;
    filter: drop-shadow(0 0 24px rgba(14, 143, 212, 0.6));
  }
}

@keyframes ring-pulse {
  0% { transform: scale(0.5); opacity: 0.8; }
  100% { transform: scale(1.5); opacity: 0; }
}

.failed-icon {
  font-size: 80px;
}

.status-title {
  font-size: $font-3xl;
  font-weight: 700;
  color: $text-primary;
  margin-bottom: $space-sm;
  letter-spacing: -0.5px;
}

.status-subtitle {
  font-size: $font-md;
  color: $text-secondary;
  line-height: 1.6;
  max-width: 280px;
  margin-bottom: $space-lg;
}

.elapsed {
  display: inline-block;
  padding: 4px 12px;
  background: $glass-bg;
  border: $glass-border;
  border-radius: $radius-full;
  font-size: $font-xs;
  color: $color-cyan;
  margin-bottom: $space-2xl;
  font-variant-numeric: tabular-nums;
}

.dots {
  display: flex;
  gap: 8px;
  margin-top: $space-lg;

  .dot {
    width: 10px;
    height: 10px;
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

.actions {
  margin-top: $space-2xl;
  width: 100%;
  max-width: 320px;
}

.error-msg {
  padding: $space-md;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.2);
  border-radius: $radius-md;
  color: $color-danger;
  font-size: $font-sm;
  margin-bottom: $space-lg;
  word-break: break-all;
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
  box-shadow: $shadow-glow;

  &:active { transform: scale(0.98); }
}
</style>
