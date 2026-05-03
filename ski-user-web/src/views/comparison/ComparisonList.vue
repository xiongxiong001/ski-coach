<template>
  <div class="page-container">
    <h2>对比报告</h2>
    <EmptyState
      icon="📊"
      title="P3.2 阶段实现"
      description="对比报告列表将在 P3.2 阶段完整实现"
    >
      <el-button type="primary" @click="router.push('/comparison/create')">立即创建对比</el-button>
    </EmptyState>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import EmptyState from '@/components/EmptyState.vue'
const router = useRouter()
</script>
