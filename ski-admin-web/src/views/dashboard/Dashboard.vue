<template>
  <div class="page-container" v-loading="loading">
    <!-- KPI 卡片 -->
    <div class="kpi-grid">
      <div class="kpi-card kpi-blue">
        <div class="kpi-icon">👥</div>
        <div class="kpi-info">
          <div class="kpi-label">总用户数</div>
          <div class="kpi-value">{{ formatNumber(overview.totalUsers) }}</div>
          <div class="kpi-trend">
            <span class="trend-text">今日新增</span>
            <span class="trend-num positive">+{{ overview.newUsersToday || 0 }}</span>
          </div>
        </div>
      </div>

      <div class="kpi-card kpi-green">
        <div class="kpi-icon">🎬</div>
        <div class="kpi-info">
          <div class="kpi-label">总视频数</div>
          <div class="kpi-value">{{ formatNumber(overview.totalVideos) }}</div>
          <div class="kpi-trend">
            <span class="trend-text">今日上传</span>
            <span class="trend-num positive">+{{ overview.videosToday || 0 }}</span>
          </div>
        </div>
      </div>

      <div class="kpi-card kpi-purple">
        <div class="kpi-icon">📄</div>
        <div class="kpi-info">
          <div class="kpi-label">总报告数</div>
          <div class="kpi-value">{{ formatNumber(overview.totalReports) }}</div>
          <div class="kpi-trend">
            <span class="trend-text">今日生成</span>
            <span class="trend-num positive">+{{ overview.reportsToday || 0 }}</span>
          </div>
        </div>
      </div>

      <div class="kpi-card kpi-orange">
        <div class="kpi-icon">💰</div>
        <div class="kpi-info">
          <div class="kpi-label">累计 LLM 花费</div>
          <div class="kpi-value">{{ formatYuan(overview.llmCostTotal) }}</div>
          <div class="kpi-trend">
            <span class="trend-text">今日花费</span>
            <span class="trend-num">{{ formatYuan(overview.llmCostToday) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 实时状态 -->
    <div class="status-row">
      <div class="status-card status-running" @click="goToTasks('running')">
        <div class="status-icon">⚙️</div>
        <div class="status-info">
          <div class="status-num">{{ overview.runningTasks || 0 }}</div>
          <div class="status-label">进行中任务</div>
        </div>
        <el-icon class="arrow-icon"><ArrowRight /></el-icon>
      </div>

      <div class="status-card status-failed" @click="goToTasks('failed')">
        <div class="status-icon">❌</div>
        <div class="status-info">
          <div class="status-num">{{ overview.failedTasks || 0 }}</div>
          <div class="status-label">失败任务</div>
        </div>
        <el-icon class="arrow-icon"><ArrowRight /></el-icon>
      </div>
    </div>

    <!-- 每日趋势 -->
    <div class="card">
      <div class="card-header">
        <div class="card-title">每日趋势</div>
        <el-radio-group v-model="trendRange" @change="loadDailyStats">
          <el-radio-button :value="7">近 7 天</el-radio-button>
          <el-radio-button :value="14">近 14 天</el-radio-button>
          <el-radio-button :value="30">近 30 天</el-radio-button>
        </el-radio-group>
      </div>
      <div ref="chartRef" class="chart-container"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import { getOverview, getDailyStats } from '@/api/stats'
import { formatNumber, formatYuan } from '@/utils/format'

const router = useRouter()

const loading = ref(false)
const overview = ref({})
const trendRange = ref(7)

const chartRef = ref()
let chartInstance = null

onMounted(async () => {
  loading.value = true
  await Promise.all([loadOverview(), loadDailyStats()])
  loading.value = false
  // 监听窗口缩放
  window.addEventListener('resize', resizeChart)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeChart)
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})

async function loadOverview() {
  try {
    overview.value = await getOverview()
  } catch (e) {}
}

async function loadDailyStats() {
  try {
    const endDate = dayjs().format('YYYY-MM-DD')
    const startDate = dayjs().subtract(trendRange.value - 1, 'day').format('YYYY-MM-DD')
    const data = await getDailyStats({ startDate, endDate })
    renderChart(data || [])
  } catch (e) {}
}

function renderChart(data) {
  nextTick(() => {
    if (!chartRef.value) return
    if (!chartInstance) {
      chartInstance = echarts.init(chartRef.value)
    }

    const dates = data.map(d => dayjs(d.statDate).format('MM-DD'))
    const newUsers = data.map(d => d.newUsers || 0)
    const videoCount = data.map(d => d.videoCount || 0)
    const taskSuccess = data.map(d => d.taskSuccess || 0)
    const llmCost = data.map(d => Number(d.llmCost || 0))

    chartInstance.setOption({
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(31, 41, 55, 0.95)',
        borderColor: 'transparent',
        textStyle: { color: '#fff' }
      },
      legend: {
        data: ['新增用户', '上传视频', '成功任务', 'LLM花费(元)'],
        bottom: 0,
        textStyle: { color: '#6B7280' }
      },
      grid: {
        left: 50,
        right: 60,
        top: 30,
        bottom: 40
      },
      xAxis: {
        type: 'category',
        data: dates,
        axisLine: { lineStyle: { color: '#E5E7EB' } },
        axisLabel: { color: '#6B7280' }
      },
      yAxis: [
        {
          type: 'value',
          name: '数量',
          nameTextStyle: { color: '#6B7280' },
          axisLine: { lineStyle: { color: '#E5E7EB' } },
          axisLabel: { color: '#6B7280' },
          splitLine: { lineStyle: { color: '#F3F4F6' } }
        },
        {
          type: 'value',
          name: '花费(元)',
          nameTextStyle: { color: '#6B7280' },
          axisLine: { lineStyle: { color: '#E5E7EB' } },
          axisLabel: { color: '#6B7280', formatter: '¥{value}' },
          splitLine: { show: false }
        }
      ],
      series: [
        {
          name: '新增用户',
          type: 'line',
          smooth: true,
          data: newUsers,
          itemStyle: { color: '#3B82F6' },
          areaStyle: { color: 'rgba(59, 130, 246, 0.1)' }
        },
        {
          name: '上传视频',
          type: 'line',
          smooth: true,
          data: videoCount,
          itemStyle: { color: '#10B981' },
          areaStyle: { color: 'rgba(16, 185, 129, 0.1)' }
        },
        {
          name: '成功任务',
          type: 'line',
          smooth: true,
          data: taskSuccess,
          itemStyle: { color: '#8B5CF6' }
        },
        {
          name: 'LLM花费(元)',
          type: 'bar',
          yAxisIndex: 1,
          data: llmCost,
          itemStyle: { color: '#F59E0B', borderRadius: [4, 4, 0, 0] },
          barWidth: '30%'
        }
      ]
    })
  })
}

function resizeChart() {
  if (chartInstance) chartInstance.resize()
}

function goToTasks(status) {
  router.push({ path: '/tasks', query: { status } })
}
</script>

<style lang="scss" scoped>
// ====== KPI 卡片 ======
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: $space-lg;
  margin-bottom: $space-lg;

  @media (max-width: 1200px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.kpi-card {
  background: white;
  border-radius: $radius-lg;
  padding: $space-lg;
  box-shadow: $shadow-sm;
  border: 1px solid $border-light;
  display: flex;
  align-items: center;
  gap: $space-lg;
  position: relative;
  overflow: hidden;
  transition: transform 0.15s, box-shadow 0.15s;

  &::before {
    content: '';
    position: absolute;
    top: -40px; right: -40px;
    width: 140px; height: 140px;
    border-radius: 50%;
    opacity: 0.08;
  }
  &:hover {
    transform: translateY(-2px);
    box-shadow: $shadow-md;
  }

  &.kpi-blue::before   { background: $color-primary; }
  &.kpi-green::before  { background: $color-success; }
  &.kpi-purple::before { background: #8B5CF6; }
  &.kpi-orange::before { background: $color-warning; }
}

.kpi-icon {
  font-size: 36px;
  flex-shrink: 0;
}

.kpi-info {
  flex: 1;
  position: relative;
  z-index: 1;
}

.kpi-label {
  font-size: $font-sm;
  color: $text-secondary;
  margin-bottom: 4px;
}

.kpi-value {
  font-size: 28px;
  font-weight: 700;
  color: $text-primary;
  line-height: 1.2;
  margin-bottom: $space-xs;
  font-variant-numeric: tabular-nums;
}

.kpi-trend {
  display: flex;
  align-items: center;
  gap: $space-sm;
  font-size: $font-xs;

  .trend-text { color: $text-secondary; }
  .trend-num {
    color: $text-primary;
    font-weight: 500;

    &.positive { color: $color-success; }
  }
}

// ====== 实时状态 ======
.status-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $space-lg;
  margin-bottom: $space-lg;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

.status-card {
  background: white;
  border-radius: $radius-lg;
  padding: $space-lg;
  box-shadow: $shadow-sm;
  border: 1px solid $border-light;
  display: flex;
  align-items: center;
  gap: $space-md;
  cursor: pointer;
  transition: all 0.15s;

  &:hover {
    border-color: $color-primary;
    box-shadow: $shadow-md;
  }

  .status-icon {
    width: 48px;
    height: 48px;
    border-radius: $radius-md;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
  }

  &.status-running .status-icon { background: rgba(245, 158, 11, 0.1); }
  &.status-failed .status-icon  { background: rgba(239, 68, 68, 0.1); }

  .status-info {
    flex: 1;
  }

  .status-num {
    font-size: $font-2xl;
    font-weight: 700;
    color: $text-primary;
  }
  .status-label {
    font-size: $font-sm;
    color: $text-secondary;
  }
  .arrow-icon {
    color: $text-placeholder;
    font-size: 16px;
  }
}

// ====== 趋势图 ======
.card {
  background: white;
  border-radius: $radius-lg;
  padding: $space-lg;
  box-shadow: $shadow-sm;
  border: 1px solid $border-light;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $space-md;

  .card-title {
    font-size: $font-lg;
    font-weight: 600;
  }
}

.chart-container {
  height: 360px;
  width: 100%;
}
</style>
