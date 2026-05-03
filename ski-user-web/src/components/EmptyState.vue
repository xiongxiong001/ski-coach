<template>
  <div class="empty-state">
    <div class="empty-icon">{{ icon }}</div>
    <div class="empty-title">{{ title }}</div>
    <div class="empty-desc">{{ description }}</div>
    <div v-if="$slots.default" class="empty-actions">
      <slot />
    </div>
  </div>
</template>

<script setup>
defineProps({
  icon: { type: String, default: '📭' },
  title: { type: String, default: '暂无数据' },
  description: { type: String, default: '' }
})
</script>

<style lang="scss" scoped>
.empty-state {
  text-align: center;
  padding: $space-2xl $space-lg;

  .empty-icon { font-size: 56px; margin-bottom: $space-md; opacity: 0.6; }
  .empty-title { font-size: 16px; color: $text-regular; margin-bottom: $space-xs; }
  .empty-desc { color: $text-secondary; font-size: 13px; margin-bottom: $space-lg; }
  .empty-actions { margin-top: $space-md; }
}
</style>
