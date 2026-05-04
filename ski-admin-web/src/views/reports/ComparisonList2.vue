<!-- ski-admin-web/src/views/reports/ComparisonList.vue -->
<template>
  <div class="comparison-list">
    <div class="search-bar">
      <el-input 
        v-model="searchForm.keyword" 
        placeholder="搜索报告内容"
        class="search-input"
        @keyup.enter="handleSearch"
      >
        <template #append>
          <el-button @click="handleSearch">搜索</el-button>
        </template>
      </el-input>
    </div>
    
    <el-table :data="reportList" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="phone" label="用户手机号" />
      <el-table-column prop="nickname" label="用户昵称" />
      <el-table-column prop="prevFilename" label="上次视频" />
      <el-table-column prop="currFilename" label="本次视频" />
      <el-table-column prop="improvedCount" label="进步指标" width="100" />
      <el-table-column prop="declinedCount" label="退步指标" width="100" />
      <el-table-column prop="reportSummary" label="报告摘要" />
      <el-table-column prop="llmCostYuan" label="LLM费用(元)" />
      <el-table-column prop="createdTime" label="创建时间" />
      <el-table-column label="操作" width="120">
        <template #default="scope">
          <el-button @click="showDetail(scope.row)">查看详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <el-pagination
      :current-page="pagination.pageNum"
      :page-size="pagination.pageSize"
      :total="pagination.total"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />
    
    <!-- 详情抽屉 -->
    <el-drawer :visible="detailVisible" direction="rtl" title="对比报告详情">
      <div v-if="detailData" class="report-detail">
        <div class="detail-header">
          <div class="info-row">
            <span class="label">用户：</span>
            <span>{{ detailData.phone }} ({{ detailData.nickname }})</span>
          </div>
          <div class="info-row">
            <span class="label">上次视频：</span>
            <span>{{ detailData.prevFilename }}</span>
          </div>
          <div class="info-row">
            <span class="label">本次视频：</span>
            <span>{{ detailData.currFilename }}</span>
          </div>
          <div class="info-row">
            <span class="label">进步/退步/稳定性：</span>
            <span>{{ detailData.improvedCount }} / {{ detailData.declinedCount }} / {{ detailData.stabilityImprovedCount }}</span>
          </div>
          <div class="info-row">
            <span class="label">费用：</span>
            <span>{{ detailData.llmCostYuan }} 元</span>
          </div>
        </div>
        <div class="detail-content" v-html="renderedMarkdown"></div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { listComparisonReports, getComparisonReportDetail } from '@/api/reports'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

const searchForm = ref({ keyword: '', userId: null })
const reportList = ref([])
const pagination = ref({ pageNum: 1, pageSize: 20, total: 0 })
const detailVisible = ref(false)
const detailData = ref(null)

const renderedMarkdown = computed(() => {
  if (!detailData.value?.reportMarkdown) return ''
  return DOMPurify.sanitize(marked(detailData.value.reportMarkdown))
})

const handleSearch = () => {
  pagination.value.pageNum = 1
  fetchReports()
}

const fetchReports = () => {
  listComparisonReports({
    ...searchForm.value,
    pageNum: pagination.value.pageNum,
    pageSize: pagination.value.pageSize
  }).then(res => {
    reportList.value = res.records
    pagination.value.total = res.total
  })
}

const showDetail = (row) => {
  getComparisonReportDetail(row.id).then(res => {
    detailData.value = res
    detailVisible.value = true
  })
}

const handleSizeChange = (size) => {
  pagination.value.pageSize = size
  fetchReports()
}

const handleCurrentChange = (page) => {
  pagination.value.pageNum = page
  fetchReports()
}

fetchReports()
</script>