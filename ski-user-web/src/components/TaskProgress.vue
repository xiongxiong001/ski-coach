<template>
  <div class="task-progress" :class="`status-${currentStatus}`">
    <div class="progress-icon">
      <el-icon v-if="currentStatus === 'success'" :size="32" color="#10b981"><CircleCheckFilled /></el-icon>
      <el-icon v-else-if="currentStatus === 'failed'" :size="32" color="#ef4444"><CircleCloseFilled /></el-icon>
      <el-icon v-else :size="32" class="loading-icon" color="#2563eb"><Loading /></el-icon>
    </div>

    <div class="progress-text">
      <div class="title">{{ statusText }}</div>
      <div class="hint">{{ statusHint }}</div>
    </div>

    <div v-if="currentStatus === 'failed' && task?.errorMessage" class="error-detail">
      失败原因:{{ task.errorMessage }}
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onUnmounted } from 'vue'
import { CircleCheckFilled, CircleCloseFilled, Loading } from '@element-plus/icons-vue'
import { getTaskStatus } from '@/api/task'
import { TASK_POLL_INTERVAL } from '@/utils/constants'

const props = defineProps({
  taskId: { type: [Number, String], required: true }
})

const emit = defineEmits(['success', 'failed'])

const task = ref(null)
const currentStatus = ref('pending')
const timer = ref(null)

const statusText = computed(() => {
  switch (currentStatus.value) {
    case 'pending': return '已加入任务队列,等待处理...'
    case 'running': return 'AI 教练正在分析视频...'
    case 'success': return '分析完成!'
    case 'failed':  return '分析失败'
    default:        return '加载中...'
  }
})

const statusHint = computed(() => {
  if (currentStatus.value === 'running') return '通常需要 1-3 分钟,请耐心等候'
  if (currentStatus.value === 'pending') return '前面还有任务在排队,稍等'
  if (currentStatus.value === 'success') return '即将跳转到报告页面'
  if (currentStatus.value === 'failed')  return '可以重新上传或联系管理员'
  return ''
})

async function fetchStatus() {
  try {
    const data = await getTaskStatus(props.taskId)
    task.value = data
    currentStatus.value = data.status

    if (data.status === 'success') {
      stopPolling()
      emit('success', data)
    } else if (data.status === 'failed') {
      stopPolling()
      emit('failed', data)
    }
  } catch (e) {
    // 出错继续轮询
  }
}

function startPolling() {
  fetchStatus()
  timer.value = setInterval(fetchStatus, TASK_POLL_INTERVAL)
}

function stopPolling() {
  if (timer.value) {
    clearInterval(timer.value)
    timer.value = null
  }
}

watch(() => props.taskId, (val) => {
  if (val) startPolling()
}, { immediate: true })

onUnmounted(() => stopPolling())
</script>

<style lang="scss" scoped>
.task-progress {
  display: flex;
  align-items: center;
  gap: $space-md;
  padding: $space-lg;
  border-radius: $radius-lg;
  border: 1px solid $border-light;
  background: $bg-card;

  &.status-success { background: #f0fdf4; border-color: #86efac; }
  &.status-failed  { background: #fef2f2; border-color: #fca5a5; }
  &.status-running, &.status-pending { background: #eff6ff; border-color: #93c5fd; }
}

.progress-icon { flex-shrink: 0; }

.loading-icon {
  animation: rotate 1.5s linear infinite;
}
@keyframes rotate {
  100% { transform: rotate(360deg); }
}

.progress-text {
  flex: 1;

  .title { font-size: 15px; font-weight: 500; color: $text-primary; margin-bottom: 2px; }
  .hint { font-size: 13px; color: $text-secondary; }
}

.error-detail {
  flex-basis: 100%;
  margin-top: $space-sm;
  padding: $space-sm;
  background: rgba(239, 68, 68, 0.05);
  border-radius: $radius-md;
  color: $color-danger;
  font-size: 13px;
}
</style>
