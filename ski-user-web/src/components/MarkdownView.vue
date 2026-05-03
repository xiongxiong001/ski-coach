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

// 配置 marked: 简洁、安全
marked.setOptions({
  breaks: true,        // 换行符自动转<br>
  gfm: true            // GitHub风格 Markdown
})

const renderedHtml = computed(() => {
  if (!props.content) return ''
  const rawHtml = marked.parse(props.content)
  // DOMPurify 清洗,防 XSS
  return DOMPurify.sanitize(rawHtml)
})
</script>
