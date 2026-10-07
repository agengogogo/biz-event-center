<template>
  <div>
    <el-card class="chart-card">
      <template #header>
        <div class="card-header">
          <span>事件统计</span>
          <div class="controls">
            <el-select v-model="dimension" style="width: 140px" @change="loadStatistics">
              <el-option label="按类型" value="TYPE" />
              <el-option label="按来源" value="SOURCE" />
              <el-option label="按严重级别" value="SEVERITY" />
              <el-option label="按位置" value="LOCATION" />
              <el-option label="按时间" value="TIME" />
            </el-select>
            <el-select v-model="interval" style="width: 120px" @change="loadStatistics">
              <el-option label="小时" value="hour" />
              <el-option label="天" value="day" />
            </el-select>
          </div>
        </div>
      </template>
      <div ref="chartRef" class="chart"></div>
    </el-card>

    <el-card class="situation-card">
      <template #header>
        <div class="card-header">
          <span>实时态势（活跃告警 {{ situation.activeCount }}）</span>
          <el-tag :type="connected ? 'success' : 'info'">
            {{ connected ? '实时推送中' : '未连接' }}
          </el-tag>
        </div>
      </template>
      <el-table :data="situation.activeAlerts" border>
        <el-table-column prop="eventType" label="事件类型" min-width="160" />
        <el-table-column prop="severity" label="级别" width="110" />
        <el-table-column prop="location" label="位置" width="140" />
        <el-table-column prop="occurredAt" label="发生时间" width="200" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { getSituation, getStatistics } from '@/api'
import type { SituationView } from '@/types'
import { TOKEN_KEY } from '@/api/http'

const chartRef = ref<HTMLDivElement | null>(null)
const dimension = ref('SEVERITY')
const interval = ref('hour')
const situation = ref<SituationView>({ activeCount: 0, activeAlerts: [] })
const connected = ref(false)

let chart: echarts.ECharts | null = null
let source: EventSource | null = null

async function loadStatistics() {
  const stats = await getStatistics(dimension.value, interval.value)
  if (!chart) {
    return
  }
  chart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: stats.points.map((point) => point.key) },
    yAxis: { type: 'value' },
    series: [
      {
        type: 'bar',
        data: stats.points.map((point) => point.total),
        itemStyle: { color: '#409eff' }
      }
    ]
  })
}

function connectStream() {
  const token = localStorage.getItem(TOKEN_KEY)
  if (!token) {
    return
  }
  source = new EventSource(`/api/dashboard/stream?access_token=${encodeURIComponent(token)}`)
  source.addEventListener('situation', (event) => {
    situation.value = JSON.parse((event as MessageEvent).data)
    connected.value = true
  })
  source.onerror = () => {
    connected.value = false
  }
}

onMounted(async () => {
  if (chartRef.value) {
    chart = echarts.init(chartRef.value)
  }
  situation.value = await getSituation()
  await loadStatistics()
  connectStream()
})

onBeforeUnmount(() => {
  source?.close()
  chart?.dispose()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.controls {
  display: flex;
  gap: 12px;
}

.chart {
  height: 360px;
}

.chart-card {
  margin-bottom: 16px;
}
</style>
