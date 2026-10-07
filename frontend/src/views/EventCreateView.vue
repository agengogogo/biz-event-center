<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>手工创建事件</span>
        <el-button @click="router.back()">返回</el-button>
      </div>
    </template>

    <el-form :model="form" label-width="120px" @submit.prevent>
      <el-form-item label="事件类型" required>
        <el-input v-model="form.eventType" placeholder="如 manual.inspection" />
      </el-form-item>
      <el-form-item label="发生时间" required>
        <el-input v-model="form.occurredAt" placeholder="ISO-8601，如 2026-01-02T03:04:05Z" />
      </el-form-item>
      <el-form-item label="严重级别">
        <el-select v-model="form.severity" style="width: 160px">
          <el-option label="INFO" value="INFO" />
          <el-option label="WARNING" value="WARNING" />
          <el-option label="MAJOR" value="MAJOR" />
          <el-option label="CRITICAL" value="CRITICAL" />
        </el-select>
      </el-form-item>
      <el-form-item label="来源类型">
        <el-select v-model="form.sourceType" style="width: 160px">
          <el-option label="MANUAL" value="MANUAL" />
          <el-option label="IOT" value="IOT" />
          <el-option label="ACCESS" value="ACCESS" />
          <el-option label="ENERGY" value="ENERGY" />
          <el-option label="PARKING" value="PARKING" />
          <el-option label="SECURITY" value="SECURITY" />
          <el-option label="THIRD_PARTY" value="THIRD_PARTY" />
        </el-select>
      </el-form-item>
      <el-form-item label="位置">
        <el-input v-model="form.location" placeholder="如 Building A" />
      </el-form-item>
      <el-form-item label="负载(JSON)">
        <el-input v-model="payloadText" type="textarea" :rows="6" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="submit">创建</el-button>
      </el-form-item>
    </el-form>

    <el-alert v-if="result" :title="result" type="success" show-icon />
  </el-card>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createEvent } from '@/api'

const router = useRouter()
const loading = ref(false)
const result = ref('')
const payloadText = ref('{}')

const form = reactive({
  eventType: '',
  occurredAt: new Date().toISOString(),
  severity: 'WARNING',
  sourceType: 'MANUAL',
  location: ''
})

async function submit() {
  if (!form.eventType) {
    ElMessage.warning('请填写事件类型')
    return
  }
  let payload: Record<string, unknown> = {}
  try {
    payload = JSON.parse(payloadText.value)
  } catch {
    ElMessage.error('负载不是合法 JSON')
    return
  }
  loading.value = true
  try {
    const response = await createEvent({ ...form, payload })
    result.value = `已创建事件 ${response.eventId}（accepted=${response.accepted}）`
    ElMessage.success('创建成功')
    router.push({ name: 'event-detail', params: { id: response.eventId } })
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
