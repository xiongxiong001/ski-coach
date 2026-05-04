<template>
  <div class="markdown-content" v-html="renderedHtml" />
</template>

<script setup>
import { computed } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

const props = defineProps({
  content: { type: String, default: '' }
})

marked.setOptions({
  breaks: true,
  gfm: true
})

const renderedHtml = computed(() => {
  if (!props.content) return ''
  const rawHtml = marked.parse(props.content)
  return DOMPurify.sanitize(rawHtml)
})
</script>

<style lang="scss" scoped>
.markdown-content {
  font-size: 14px;
  line-height: 1.75;
  color: $text-primary;
  word-break: break-word;

  :deep(h1), :deep(h2), :deep(h3), :deep(h4) {
    margin-top: $space-lg;
    margin-bottom: $space-md;
    font-weight: 600;
    line-height: 1.3;
  }
  :deep(h1) {
    font-size: 22px;
    color: $color-primary-dark;
    padding-bottom: $space-sm;
    border-bottom: 2px solid $color-primary;
  }
  :deep(h2) {
    font-size: 18px;
    color: $color-primary-dark;
  }
  :deep(h3) {
    font-size: 16px;
    color: $text-primary;
  }
  :deep(h4) {
    font-size: 15px;
    color: $text-regular;
  }

  :deep(p) { margin-bottom: $space-md; }

  :deep(ul), :deep(ol) {
    margin-bottom: $space-md;
    padding-left: 24px;
  }
  :deep(li) {
    margin-bottom: $space-xs;
  }

  :deep(strong) {
    color: $color-primary-dark;
    font-weight: 600;
  }
  :deep(em) {
    color: $color-warning;
    font-style: normal;
  }

  :deep(code) {
    background: $bg-page;
    color: $color-primary;
    padding: 2px 6px;
    border-radius: $radius-sm;
    font-family: "SF Mono", Consolas, monospace;
    font-size: 13px;
  }

  :deep(blockquote) {
    margin: $space-md 0;
    padding: $space-sm $space-md;
    border-left: 3px solid $color-primary;
    background: $bg-page;
    color: $text-regular;
  }

  :deep(hr) {
    margin: $space-lg 0;
    border: none;
    border-top: 1px solid $border-light;
  }
}
</style>
