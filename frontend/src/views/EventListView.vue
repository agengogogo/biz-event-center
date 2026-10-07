<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>事件列表</span>
        <el-button type="primary" @click="router.push({ name: 'event-create' })">手工创建事件</el-button>
      </div>
    </template>

    <el-form :inline="true" class="filters" @submit.prevent>
      <el-form-item label="类型">
        <el-input v-model="filters.type" placeholder="如 access.denied" clearable />
      </el-form-item>
      <el-form-item label="严重级别">
        <el-select v-model="filters.severity" placeholder="全部" clearable style="width: 140px">
          <el-option label="INFO" value="INFO" />
          <el-option label="WARNING" value="WARNING" />
          <el-option label="MAJOR" value="MAJOR" />
          <el-option label="CRITICAL" value="CRITICAL" />
        </el-select>
      </el-form-item>
      <el-form-item label="来源">
        <el-input v-model="filters.source" placeholder="来源标识" clearable />
      </el-form-item>
      <el-form-item label="位置">
        <el-input v-model="filters.location" placeholder="位置关键字" clearable />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadEvents">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="events" border>
      <el-table-column prop="eventId" label="事件ID" width="220">
        <template #default="{ row }">
          <el-link type="primary" @click="openDetail(row.eventId)">{{ row.eventId }}</el-link>
        </template>
      </el-table-column>
      <el-table-column prop="eventType" label="事件类型" min-width="160" />
      <el-table-column prop="severity" label="严重级别" width="110">
        <template #default="{ row }">
          <el-tag :type="severityType(row.severity)">{{ row.severity }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="source" label="来源" width="140" />
      <el-table-column prop="occurredAt" label="发生时间" width="200" />
      <el-table-column prop="location" label="位置" width="140" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag type="info">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleDispatch(row.eventId)">分发</el-button>
          <el-button link type="warning" @click="handleRedeliver(row.eventId)">重投</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="pagination"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="pageSize"
      :current-page="page + 1"
      @current-change="handlePageChange"
    />
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { dispatchEvent, listEvents, redeliverEvent } from '@/api'
import type { EventView } from '@/types'

const router = useRouter()
const events = ref<EventView[]>([])
const total = ref(0)
const page = ref(0)
const pageSize = ref(20)
const loading = ref(false)

const filters = reactive({ type: '', severity: '', source: '', location: '' })

function severityType(severity: string): 'info' | 'warning' | 'danger' {
  if (severity === 'CRITICAL' || severity === 'MAJOR') {
    return 'danger'
  }
  if (severity === 'WARNING') {
    return 'warning'
  }
  return 'info'
}

async function loadEvents() {
  loading.value = true
  try {
    const data = await listEvents({
      ...filters,
      page: page.value,
      size: pageSize.value
    })
    events.value = data.items
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.type = ''
  filters.severity = ''
  filters.source = ''
  filters.location = ''
  page.value = 0
  loadEvents()
}

function handlePageChange(current: number) {
  page.value = current - 1
  loadEvents()
}

function openDetail(eventId: string) {
  router.push({ name: 'event-detail', params: { id: eventId } })
}

async function handleDispatch(eventId: string) {
  const result = await dispatchEvent(eventId)
  ElMessage.success(`分发完成：投递 ${result.delivered}，失败 ${result.failed}`)
  loadEvents()
}

async function handleRedeliver(eventId: string) {
  const result = await redeliverEvent(eventId)
  ElMessage.success(`重投完成：投递 ${result.delivered}，死信 ${result.deadLettered}`)
  loadEvents()
}

onMounted(loadEvents)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filters {
  margin-bottom: 8px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
