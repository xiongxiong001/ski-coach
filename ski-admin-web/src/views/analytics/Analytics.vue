<template>
  <div class="page-container" v-loading="loading">
    <!-- LLM 成本分析 -->
    <div class="grid-2">
      <div class="card">
        <div class="card-header">
          <div class="card-title">LLM 调用次数(按任务类型)</div>
        </div>
        <div ref="callChartRef" class="chart-container"></div>
      </div>

      <div class="card">
        <div class="card-header">
          <div class="card-title">LLM 花费分布(按任务类型)</div>
        </div>
        <div ref="costChartRef" class="chart-container"></div>
      </div>
    </div>

    <!-- 详细成本表格 -->
    <div class="card mt-md">
      <div class="card-header">
        <div class="card-title">详细成本明细</div>
      </div>
      <el-table :data="llmCostList" stripe style="width: 100%">
        <el-table-column label="任务类型" min-width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="row.taskType === 'single' ? 'primary' : 'success'" effect="plain">
              {{ row.taskType === 'single' ? '单次分析' : '对比分析' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="callCount" label="调用次数" align="right" min-width="100">
          <template #default="{ row }">
            <span class="num-cell">{{ formatNumber(row.callCount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalInputTokens" label="输入 tokens" align="right" min-width="130">
          <template #default="{ row }">
            <span class="num-cell">{{ formatNumber(row.totalInputTokens) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalOutputTokens" label="输出 tokens" align="right" min-width="130">
          <template #default="{ row }">
            <span class="num-cell">{{ formatNumber(row.totalOutputTokens) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalCost" label="总花费" align="right" min-width="120">
          <template #default="{ row }">
            <span class="num-cell cost-cell">{{ formatYuan(row.totalCost) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="avgCost" label="平均单次" align="right" min-width="120">
          <template #default="{ row }">
            <span class="num-cell">{{ formatYuan(row.avgCost) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 存储使用 -->
    <div class="grid-2 mt-md">
      <div class="card">
        <div class="card-header">
          <div class="card-title">存储使用情况</div>
        </div>
        <div v-if="storage" class="storage-info">
          <div class="storage-row">
            <span class="storage-label">存储路径</span>
            <span class="storage-value mono">{{ storage.basePath }}</span>
          </div>
          <div class="storage-row">
            <span class="storage-label">活跃视频</span>
            <span class="storage-value">{{ formatNumber(storage.activeVideos) }} 个</span>
          </div>
          <div class="storage-row">
            <span class="storage-label">已删除视频</span>
            <span class="storage-value">{{ formatNumber(storage.deletedVideos) }} 个</span>
          </div>
          <div class="storage-row">
            <span class="storage-label">视频总大小</span>
            <span class="storage-value highlight">{{ storage.totalSizeGB }}</span>
          </div>
          <div class="storage-row">
            <span class="storage-label">磁盘可用</span>
            <span class="storage-value">{{ storage.freeSpaceGB }}</span>
          </div>
          <div class="storage-row">
            <span class="storage-label">目录状态</span>
            <span class="storage-value">
              <el-tag v-if="storage.storageDirExists" type="success" size="small">正常</el-tag>
              <el-tag v-else type="danger" size="small">不存在</el-tag>
            </span>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <div class="card-title">存储占用比例</div>
        </div>
        <div ref="storageChartRef" class="chart-container"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getLlmCostStats, getStorageStats } from '@/api/stats'
import { formatNumber, formatYuan } from '@/utils/format'

const loading = ref(false)
const llmCostList = ref([])
const storage = ref(null)

const callChartRef = ref()
const costChartRef = ref()
const storageChartRef = ref()

let callChart = null
let costChart = null
let storageChart = null

onMounted(async () => {
  loading.value = true
  await Promise.all([loadLlmCost(), loadStorage()])
  loading.value = false
  window.addEventListener('resize', resizeAll)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeAll)
  ;[callChart, costChart, storageChart].forEach(c => c?.dispose())
})

async function loadLlmCost() {
  try {
    const data = await getLlmCostStats()
    llmCostList.value = data || []
    renderLlmCharts(data || [])
  } catch (e) {}
}

async function loadStorage() {
  try {
    storage.value = await getStorageStats()
    renderStorageChart()
  } catch (e) {}
}

function renderLlmCharts(data) {
  nextTick(() => {
    if (!callChartRef.value || !costChartRef.value) return

    const labelMap = { single: '单次分析', comparison: '对比分析' }
    const colorMap = { single: '#3B82F6', comparison: '#10B981' }

    if (!callChart) callChart = echarts.init(callChartRef.value)
    callChart.setOption({
      tooltip: {
        trigger: 'item',
        formatter: '{a} <br/>{b}: {c} 次 ({d}%)'
      },
      legend: { bottom: 0, textStyle: { color: '#6B7280' } },
      series: [{
        name: '调用次数',
        type: 'pie',
        radius: ['45%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}\n{d}%', color: '#1F2937' },
        data: data.map(d => ({
          value: d.callCount,
          name: labelMap[d.taskType] || d.taskType,
          itemStyle: { color: colorMap[d.taskType] || '#6B7280' }
        }))
      }]
    })

    if (!costChart) costChart = echarts.init(costChartRef.value)
    costChart.setOption({
      tooltip: {
        trigger: 'item',
        formatter: '{a} <br/>{b}: ¥{c} ({d}%)'
      },
      legend: { bottom: 0, textStyle: { color: '#6B7280' } },
      series: [{
        name: 'LLM 花费',
        type: 'pie',
        radius: ['45%', '70%'],
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}\n¥{c}', color: '#1F2937' },
        data: data.map(d => ({
          value: Number(d.totalCost || 0).toFixed(2),
          name: labelMap[d.taskType] || d.taskType,
          itemStyle: { color: colorMap[d.taskType] || '#6B7280' }
        }))
      }]
    })
  })
}

function renderStorageChart() {
  nextTick(() => {
    if (!storageChartRef.value || !storage.value) return
    if (!storageChart) storageChart = echarts.init(storageChartRef.value)

    const used = storage.value.totalSizeBytes || 0
    const free = storage.value.freeSpaceBytes || 0
    const totalGB = ((used + free) / (1024 ** 3)).toFixed(2)

    storageChart.setOption({
      tooltip: {
        trigger: 'item',
        formatter: '{a}<br/>{b}: {c} GB'
      },
      series: [{
        name: '存储',
        type: 'pie',
        radius: ['55%', '80%'],
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        data: [
          {
            value: (used / (1024 ** 3)).toFixed(2),
            name: '已用',
            itemStyle: { color: '#3B82F6' }
          },
          {
            value: (free / (1024 ** 3)).toFixed(2),
            name: '可用',
            itemStyle: { color: '#E5E7EB' }
          }
        ]
      }],
      graphic: [
        {
          type: 'text',
          left: 'center',
          top: '40%',
          style: {
            text: storage.value.totalSizeGB,
            fill: '#1F2937',
            fontSize: 24,
            fontWeight: 700
          }
        },
        {
          type: 'text',
          left: 'center',
          top: '52%',
          style: {
            text: `已用 / 共 ${totalGB} GB`,
            fill: '#6B7280',
            fontSize: 12
          }
        }
      ]
    })
  })
}

function resizeAll() {
  callChart?.resize()
  costChart?.resize()
  storageChart?.resize()
}
</script>

<style lang="scss" scoped>
.grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: $space-md;

  @media (max-width: 1024px) {
    grid-template-columns: 1fr;
  }
}

.card {
  background: white;
  border-radius: $radius-lg;
  padding: $space-lg;
  box-shadow: $shadow-sm;
  border: 1px solid $border-light;
}

.card-header {
  margin-bottom: $space-md;
  display: flex;
  justify-content: space-between;
  align-items: center;

  .card-title {
    font-size: $font-md;
    font-weight: 600;
  }
}

.chart-container {
  height: 300px;
  width: 100%;
}

.mt-md { margin-top: $space-md; }

.num-cell {
  font-variant-numeric: tabular-nums;
  font-weight: 500;
}

.cost-cell {
  color: $color-warning;
  font-weight: 600;
}

// 存储信息
.storage-info {
  padding: $space-sm 0;
}

.storage-row {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid $border-light;
  font-size: $font-sm;

  &:last-child { border-bottom: none; }

  .storage-label {
    width: 110px;
    color: $text-secondary;
  }
  .storage-value {
    flex: 1;
    color: $text-primary;

    &.mono {
      font-family: "SF Mono", Consolas, monospace;
      font-size: $font-xs;
      background: $bg-page;
      padding: 4px 8px;
      border-radius: $radius-sm;
      word-break: break-all;
    }
    &.highlight {
      color: $color-primary;
      font-weight: 600;
      font-size: $font-md;
    }
  }
}
</style>
