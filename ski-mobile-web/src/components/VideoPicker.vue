<template>
  <div class="picker">
    <!-- 顶部 -->
    <div class="picker-header">
      <div class="picker-handle"></div>
      <div class="header-row">
        <h3 class="picker-title">{{ title }}</h3>
        <button class="close-btn" @click="$emit('close')">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M6 6l12 12M6 18L18 6" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
          </svg>
        </button>
      </div>
      <div class="picker-tip">仅显示已完成分析的视频</div>
    </div>

    <!-- 列表 -->
    <div class="picker-list" v-if="!loading && list.length">
      <div
        v-for="v in list"
        :key="v.id"
        class="video-item"
        @click="$emit('pick', v)"
      >
        <div class="thumb" :class="`thumb-${v.id % 5}`">
          <span class="emoji">🎿</span>
          <span v-if="v.durationSeconds" class="duration">{{ formatDuration(v.durationSeconds) }}</span>
        </div>
        <div class="info">
          <div class="name">{{ v.originalFilename }}</div>
          <div class="meta">
            <span>{{ formatDate(v.createdTime) }}</span>
          </div>
          <div class="stats">
            <span class="stat">检测率 <strong>{{ formatPercent(v.detectionRate) }}</strong></span>
            <span class="stat">·  左转 <strong>{{ v.turnLeftCount || 0 }}</strong></span>
            <span class="stat">·  右转 <strong>{{ v.turnRightCount || 0 }}</strong></span>
          </div>
        </div>
        <div class="check-icon">›</div>
      </div>
    </div>

    <!-- 加载 -->
    <div v-else-if="loading" class="loading">
      <div v-for="i in 3" :key="i" class="skeleton" style="height: 88px; border-radius: 12px; margin-bottom: 12px;"></div>
    </div>

    <!-- 空 -->
    <div v-else class="empty">
      <div class="empty-emoji">🎬</div>
      <div class="empty-text">还没有已完成分析的视频</div>
      <div class="empty-desc">先在「我的视频」上传并等待分析完成</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listVideos } from '@/api/video'
import { formatDate, formatDuration, formatPercent } from '@/utils/format'

const props = defineProps({
  title: { type: String, default: '选择视频' },
  excludeId: { type: [Number, String], default: null }
})

defineEmits(['pick', 'close'])

const loading = ref(true)
const list = ref([])

onMounted(async () => {
  try {
    // 只查询已分析完成的
    const data = await listVideos({
      pageNum: 1,
      pageSize: 50,
      analysisStatus: 'analyzed'
    })
    let records = data.records || []
    if (props.excludeId) {
      records = records.filter(v => v.id !== props.excludeId)
    }
    list.value = records
  } finally {
    loading.value = false
  }
})
</script>

<style lang="scss" scoped>
.picker {
  height: 100%;
  background: $bg-card;
  border-top-left-radius: $radius-2xl;
  border-top-right-radius: $radius-2xl;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.picker-header {
  padding: $space-md $space-lg $space-md;
  border-bottom: 1px solid $border-light;
  flex-shrink: 0;
}

.picker-handle {
  width: 40px;
  height: 4px;
  background: $border-base;
  border-radius: 2px;
  margin: 0 auto $space-md;
}

.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;

  .picker-title {
    font-size: $font-lg;
    font-weight: 600;
    color: $text-primary;
  }
  .close-btn {
    width: 32px; height: 32px;
    border-radius: 50%;
    background: $bg-elevated;
    color: $text-secondary;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
  }
}

.picker-tip {
  margin-top: $space-xs;
  font-size: $font-xs;
  color: $text-secondary;
}

.picker-list {
  flex: 1;
  overflow-y: auto;
  padding: $space-md $space-lg $space-2xl;
  -webkit-overflow-scrolling: touch;
}

.video-item {
  display: flex;
  gap: $space-md;
  padding: $space-md;
  background: $bg-elevated;
  border-radius: $radius-md;
  border: 1px solid $border-light;
  margin-bottom: $space-md;
  cursor: pointer;
  transition: all 0.15s;

  &:active {
    border-color: $color-primary;
    transform: scale(0.99);
  }
}

.thumb {
  width: 80px;
  height: 60px;
  border-radius: $radius-sm;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  flex-shrink: 0;

  &.thumb-0 { background: linear-gradient(135deg, #1e3a8a, #6d28d9); }
  &.thumb-1 { background: linear-gradient(135deg, #075985, #0c4a6e); }
  &.thumb-2 { background: linear-gradient(135deg, #4c1d95, #7e22ce); }
  &.thumb-3 { background: linear-gradient(135deg, #134e4a, #0f766e); }
  &.thumb-4 { background: linear-gradient(135deg, #831843, #be185d); }

  .emoji { font-size: 28px; }
  .duration {
    position: absolute;
    bottom: 2px; right: 2px;
    background: rgba(0,0,0,0.6);
    color: white;
    font-size: 10px;
    padding: 1px 4px;
    border-radius: 2px;
  }
}

.info {
  flex: 1;
  min-width: 0;
}

.name {
  font-size: $font-sm;
  color: $text-primary;
  font-weight: 500;
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.meta {
  font-size: $font-xs;
  color: $text-secondary;
  margin-bottom: 4px;
}

.stats {
  font-size: $font-xs;
  color: $text-secondary;
  display: flex;
  flex-wrap: wrap;
  gap: 2px;

  strong {
    color: $color-cyan;
    font-weight: 600;
  }
}

.check-icon {
  font-size: 22px;
  color: $text-secondary;
  align-self: center;
}

.loading {
  padding: $space-md $space-lg;
}

.empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px $space-lg;

  .empty-emoji { font-size: 56px; margin-bottom: $space-md; opacity: 0.5; }
  .empty-text {
    font-size: $font-md;
    color: $text-regular;
    margin-bottom: 4px;
    font-weight: 500;
  }
  .empty-desc {
    font-size: $font-sm;
    color: $text-secondary;
    text-align: center;
  }
}
</style>
