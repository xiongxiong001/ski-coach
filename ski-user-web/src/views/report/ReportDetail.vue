<template>
  <div class="page-container">
    <div class="back-bar">
      <el-button link @click="router.back()">
        <el-icon><ArrowLeft /></el-icon>&nbsp;返回
      </el-button>
    </div>

    <div v-loading="loading" class="report-card card">
      <div v-if="report">
        <div class="report-header">
          <div class="report-icon">📄</div>
          <div>
            <h2>AI 教练报告</h2>
            <div class="text-secondary">
              生成于 {{ formatDateTime(report.createdTime) }}
            </div>
          </div>
        </div>

        <el-divider />

        <MarkdownView :content="report.reportMarkdown" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
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
</script>

<style lang="scss" scoped>
.back-bar { margin-bottom: $space-md; }

.report-card {
  padding: $space-xl;
}

.report-header {
  display: flex;
  align-items: center;
  gap: $space-md;

  .report-icon {
    font-size: 40px;
  }

  h2 {
    font-size: 22px;
    font-weight: 600;
    margin-bottom: 4px;
  }
}
</style>
