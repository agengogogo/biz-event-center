<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>事件详情</span>
        <el-button @click="router.back()">返回</el-button>
      </div>
    </template>

    <el-descriptions v-if="event" :column="2" border>
      <el-descriptions-item label="事件ID">{{ event.eventId }}</el-descriptions-item>
      <el-descriptions-item label="事件类型">{{ event.eventType }}</el-descriptions-item>
      <el-descriptions-item label="来源">{{ event.source }}</el-descriptions-item>
      <el-descriptions-item label="来源类型">{{ event.sourceType }}</el-descriptions-item>
      <el-descriptions-item label="严重级别">{{ event.severity }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ event.status }}</el-descriptions-item>
      <el-descriptions-item label="园区">{{ event.parkId }}</el-descriptions-item>
      <el-descriptions-item label="位置">{{ event.location }}</el-descriptions-item>
      <el-descriptions-item label="发生时间">{{ event.occurredAt }}</el-descriptions-item>
      <el-descriptions-item label="traceId">{{ event.traceId }}</el-descriptions-item>
      <el-descriptions-item label="负载" :span="2">
        <pre class="payload">{{ JSON.stringify(event.payload, null, 2) }}</pre>
      </el-descriptions-item>
    </el-descriptions>

    <el-divider content-position="left">处理轨迹</el-divider>
    <el-timeline>
      <el-timeline-item
        v-for="trace in traces"
        :key="trace.occurredAt + trace.stage"
        :timestamp="trace.occurredAt"
        placement="top"
      >
        <strong>{{ trace.stage }}</strong>
        <span v-if="trace.status"> · {{ trace.status }}</span>
        <div v-if="trace.detail" class="detail">{{ trace.detail }}</div>
      </el-timeline-item>
    </el-timeline>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getEvent, getTraces } from '@/api'
import type { EventView, TraceView } from '@/types'

const route = useRoute()
const router = useRouter()
const event = ref<EventView | null>(null)
const traces = ref<TraceView[]>([])

onMounted(async () => {
  const eventId = route.params.id as string
  event.value = await getEvent(eventId)
  traces.value = await getTraces(eventId)
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.payload {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}

.detail {
  color: #909399;
  font-size: 12px;
}
</style>
