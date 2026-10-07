<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>订阅与规则</span>
        <el-button type="primary" @click="openCreate">新建订阅</el-button>
      </div>
    </template>

    <el-table v-loading="loading" :data="subscriptions" border>
      <el-table-column prop="name" label="名称" min-width="140" />
      <el-table-column prop="eventType" label="主题" width="150" />
      <el-table-column prop="targetType" label="目标类型" width="130" />
      <el-table-column prop="filterExpression" label="过滤表达式" min-width="220" />
      <el-table-column prop="enabled" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'">
            {{ row.enabled ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button link type="primary" @click="toggle(row)">
            {{ row.enabled ? '停用' : '启用' }}
          </el-button>
          <el-button link @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑订阅' : '新建订阅'" width="640px">
      <el-form :model="form" label-width="120px">
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="主题">
          <el-input v-model="form.eventType" placeholder="支持 * 或 access.*" />
        </el-form-item>
        <el-form-item label="目标类型">
          <el-select v-model="form.targetType" style="width: 200px">
            <el-option label="WEBHOOK" value="WEBHOOK" />
            <el-option label="HTTP" value="HTTP" />
            <el-option label="NOTIFICATION" value="NOTIFICATION" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标地址">
          <el-input v-model="form.targetUrl" placeholder="WEBHOOK/HTTP 时必填" />
        </el-form-item>
        <el-form-item label="通知通道ID">
          <el-input v-model="form.notificationChannelId" placeholder="NOTIFICATION 时必填" />
        </el-form-item>
        <el-form-item label="模板ID">
          <el-input v-model="form.templateId" />
        </el-form-item>
        <el-form-item label="过滤表达式">
          <el-input
            v-model="form.filterExpression"
            placeholder="如 severity == 'MAJOR' and contains(location, 'Gate')"
          />
        </el-form-item>
        <el-form-item label="转换规则(JSON)">
          <el-input v-model="rulesText" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="最大重试">
          <el-input-number v-model="form.maxRetries" :min="0" :max="10" />
        </el-form-item>
        <el-form-item label="退避毫秒">
          <el-input-number v-model="form.backoffMillis" :min="0" :step="500" />
        </el-form-item>
        <el-form-item label="有序投递">
          <el-switch v-model="form.orderedDelivery" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createSubscription,
  deleteSubscription,
  listSubscriptions,
  setSubscriptionEnabled,
  updateSubscription
} from '@/api'
import type { SubscriptionView, TransformRule } from '@/types'

const subscriptions = ref<SubscriptionView[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const editingId = ref<string | null>(null)
const rulesText = ref('[]')

const form = reactive({
  name: '',
  eventType: '*',
  targetType: 'WEBHOOK',
  targetUrl: '',
  notificationChannelId: '',
  templateId: '',
  filterExpression: '',
  maxRetries: 3,
  backoffMillis: 1000,
  orderedDelivery: false
})

async function load() {
  loading.value = true
  try {
    subscriptions.value = await listSubscriptions()
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.name = ''
  form.eventType = '*'
  form.targetType = 'WEBHOOK'
  form.targetUrl = ''
  form.notificationChannelId = ''
  form.templateId = ''
  form.filterExpression = ''
  form.maxRetries = 3
  form.backoffMillis = 1000
  form.orderedDelivery = false
  rulesText.value = '[]'
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: SubscriptionView) {
  editingId.value = row.id
  form.name = row.name
  form.eventType = row.eventType
  form.targetType = row.targetType
  form.targetUrl = row.targetUrl ?? ''
  form.notificationChannelId = row.notificationChannelId ?? ''
  form.templateId = row.templateId ?? ''
  form.filterExpression = row.filterExpression ?? ''
  form.maxRetries = row.maxRetries
  form.backoffMillis = row.backoffMillis
  form.orderedDelivery = row.orderedDelivery
  rulesText.value = JSON.stringify(row.transformRules ?? [], null, 2)
  dialogVisible.value = true
}

async function save() {
  let transformRules: TransformRule[] = []
  try {
    transformRules = JSON.parse(rulesText.value)
  } catch {
    ElMessage.error('转换规则不是合法 JSON')
    return
  }
  const payload = {
    ...form,
    targetUrl: form.targetUrl || null,
    notificationChannelId: form.notificationChannelId || null,
    templateId: form.templateId || null,
    filterExpression: form.filterExpression || null,
    transformRules,
    enabled: true
  }
  if (editingId.value) {
    await updateSubscription(editingId.value, payload)
  } else {
    await createSubscription(payload)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function toggle(row: SubscriptionView) {
  await setSubscriptionEnabled(row.id, !row.enabled)
  load()
}

async function remove(id: string) {
  await ElMessageBox.confirm('确认删除该订阅？', '提示', { type: 'warning' })
  await deleteSubscription(id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
