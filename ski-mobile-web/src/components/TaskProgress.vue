<template>
  <div class="task-progress" :class="`status-${currentStatus}`">
    <!-- 分析中 -->
    <template v-if="currentStatus === 'pending' || currentStatus === 'running'">
      <div class="ai-icon-wrap">
        <div class="ai-pulse"></div>
        <div class="ai-pulse delay-1"></div>
        <div class="ai-pulse delay-2"></div>
        <div class="ai-icon">🤖</div>
      </div>
      <div class="title">{{ statusTitle }}</div>
      <div class="subtitle">{{ statusSubtitle }}</div>
      <div class="dots">
        <div class="dot"></div>
        <div class="dot"></div>
        <div class="dot"></div>
      </div>
      <div class="elapsed">已等待 {{ elapsedText }}</div>
    </template>

    <!-- 成功 -->
    <template v-else-if="currentStatus === 'success'">
      <div class="success-icon">✓</div>
      <div class="title">分析完成!</div>
      <div class="subtitle">即将跳转到结果页...</div>
    </template>

    <!-- 失败 -->
    <template v-else-if="currentStatus === 'failed'">
      <div class="failed-icon">!</div>
      <div class="title">分析失败</div>
      <div class="failed-msg">{{ task?.errorMessage || '未知错误' }}</div>
      <div class="actions">
        <button class="action-btn" @click="$emit('retry')">重新尝试</button>
        <button class="action-btn ghost" @click="$emit('close')">返回</button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, watch, onUnmounted } from 'vue'
import { getTaskStatus } from '@/api/task'
import { TASK_POLL_INTERVAL } from '@/utils/constants'

const props = defineProps({
  taskId: { type: [Number, String], required: true },
  type: { type: String, default: 'single' }   // 'single' or 'comparison'
})

const emit = defineEmits(['success', 'failed', 'retry', 'close'])

const task = ref(null)
const currentStatus = ref('pending')
const timer = ref(null)
const startTime = ref(Date.now())
const elapsedSec = ref(0)
const elapsedTimer = ref(null)

const statusTitle = computed(() => {
  if (currentStatus.value === 'pending') return '排队中,马上开始'
  if (props.type === 'comparison') return 'AI 教练正在对比分析'
  return 'AI 教练正在分析视频'
})

const statusSubtitle = computed(() => {
  if (props.type === 'comparison') return '通常需要 30-60 秒'
  return '通常需要 1-3 分钟'
})

const elapsedText = computed(() => {
  const m = Math.floor(elapsedSec.value / 60)
  const s = elapsedSec.value % 60
  if (m === 0) return `${s} 秒`
  return `${m} 分 ${s} 秒`
})

async function fetchStatus() {
  try {
    const data = await getTaskStatus(props.taskId)
    task.value = data
    currentStatus.value = data.status

    if (data.status === 'success') {
      stopAll()
      setTimeout(() => emit('success', data), 800)
    } else if (data.status === 'failed') {
      stopAll()
      emit('failed', data)
    }
  } catch (e) {}
}

function startPolling() {
  startTime.value = Date.now()
  elapsedSec.value = 0
  fetchStatus()
  timer.value = setInterval(fetchStatus, TASK_POLL_INTERVAL)
  elapsedTimer.value = setInterval(() => {
    elapsedSec.value = Math.floor((Date.now() - startTime.value) / 1000)
  }, 1000)
}

function stopAll() {
  if (timer.value) {
    clearInterval(timer.value)
    timer.value = null
  }
  if (elapsedTimer.value) {
    clearInterval(elapsedTimer.value)
    elapsedTimer.value = null
  }
}

watch(() => props.taskId, (val) => {
  if (val) startPolling()
}, { immediate: true })

onUnmounted(stopAll)
</script>

<style lang="scss" scoped>
.task-progress {
  padding: 60px $space-2xl;
  text-align: center;
  position: relative;
  overflow: hidden;
}

.ai-icon-wrap {
  position: relative;
  width: 96px;
  height: 96px;
  margin: 0 auto $space-xl;
  display: flex;
  align-items: center;
  justify-content: center;

  .ai-pulse {
    position: absolute;
    inset: 0;
    border: 2px solid $color-primary;
    border-radius: 50%;
    animation: ai-pulse 2.4s infinite;
    opacity: 0;
  }
  .delay-1 { animation-delay: 0.8s; }
  .delay-2 { animation-delay: 1.6s; }

  .ai-icon {
    font-size: 48px;
    z-index: 2;
  }
}

@keyframes ai-pulse {
  0% { transform: scale(0.6); opacity: 0.8; }
  100% { transform: scale(1.6); opacity: 0; }
}

.title {
  font-size: $font-xl;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: $space-sm;
}

.subtitle {
  font-size: $font-sm;
  color: $text-secondary;
  margin-bottom: $space-md;
}

.dots {
  display: flex;
  justify-content: center;
  gap: 6px;
  margin-bottom: $space-lg;

  .dot {
    width: 8px; height: 8px;
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

.elapsed {
  font-size: $font-xs;
  color: $text-secondary;
  font-feature-settings: "tnum";
}

.success-icon, .failed-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto $space-lg;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  font-weight: 700;
  color: white;
}

.success-icon {
  background: linear-gradient(135deg, #10B981, #059669);
  box-shadow: 0 0 24px rgba(16, 185, 129, 0.5);
  animation: scale-in 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.failed-icon {
  background: linear-gradient(135deg, #EF4444, #DC2626);
  box-shadow: 0 0 24px rgba(239, 68, 68, 0.4);
  animation: scale-in 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes scale-in {
  from { transform: scale(0); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}

.failed-msg {
  font-size: $font-sm;
  color: $text-regular;
  background: rgba(239, 68, 68, 0.1);
  padding: $space-md;
  border-radius: $radius-md;
  margin: $space-md 0 $space-lg;
  word-break: break-all;
}

.actions {
  display: flex;
  gap: $space-md;
  justify-content: center;
}

.action-btn {
  padding: 10px 24px;
  background: $gradient-primary;
  color: white;
  border-radius: $radius-full;
  font-size: $font-sm;
  font-weight: 500;
  cursor: pointer;
  box-shadow: $shadow-glow;

  &.ghost {
    background: $bg-card;
    border: 1px solid $border-base;
    box-shadow: none;
  }
  &:active { transform: scale(0.96); }
}
</style>
