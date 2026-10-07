<template>
  <el-card>
    <el-tabs v-model="activeTab">
      <el-tab-pane label="通知通道" name="channels">
        <div class="toolbar">
          <el-button type="primary" @click="channelDialog = true">新建通道</el-button>
        </div>
        <el-table :data="channels" border>
          <el-table-column prop="name" label="名称" min-width="160" />
          <el-table-column prop="type" label="类型" width="140" />
          <el-table-column prop="config" label="配置" min-width="240">
            <template #default="{ row }">{{ JSON.stringify(row.config) }}</template>
          </el-table-column>
          <el-table-column prop="enabled" label="状态" width="90">
            <template #default="{ row }">{{ row.enabled ? '启用' : '停用' }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="通知模板" name="templates">
        <div class="toolbar">
          <el-button type="primary" @click="templateDialog = true">新建模板</el-button>
        </div>
        <el-table :data="templates" border>
          <el-table-column prop="name" label="名称" min-width="160" />
          <el-table-column prop="channelType" label="通道类型" width="140" />
          <el-table-column prop="subject" label="主题" min-width="180" />
          <el-table-column prop="body" label="内容" min-width="260" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="投递记录" name="deliveries">
        <div class="toolbar">
          <el-input
            v-model="eventIdFilter"
            placeholder="按事件ID过滤"
            clearable
            style="width: 260px"
            @clear="loadDeliveries"
          />
          <el-button @click="loadDeliveries">查询</el-button>
        </div>
        <el-table :data="deliveries" border>
          <el-table-column prop="eventId" label="事件ID" min-width="200" />
          <el-table-column prop="channelId" label="通道" width="160" />
          <el-table-column prop="status" label="状态" width="100" />
          <el-table-column prop="attempts" label="尝试" width="80" />
          <el-table-column prop="error" label="失败原因" min-width="200" />
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button link type="primary" @click="resend(row.id)">重发</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="channelDialog" title="新建通知通道" width="460px">
      <el-form :model="channelForm" label-width="90px">
        <el-form-item label="名称">
          <el-input v-model="channelForm.name" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="channelForm.type" style="width: 100%">
            <el-option label="WEBHOOK" value="WEBHOOK" />
            <el-option label="WECHAT_WORK" value="WECHAT_WORK" />
          </el-select>
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="channelForm.url" placeholder="https://..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="channelDialog = false">取消</el-button>
        <el-button type="primary" @click="saveChannel">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="templateDialog" title="新建通知模板" width="520px">
      <el-form :model="templateForm" label-width="90px">
        <el-form-item label="名称">
          <el-input v-model="templateForm.name" />
        </el-form-item>
        <el-form-item label="通道类型">
          <el-input v-model="templateForm.channelType" placeholder="WEBHOOK / WECHAT_WORK" />
        </el-form-item>
        <el-form-item label="主题">
          <el-input v-model="templateForm.subject" placeholder="[${severity}] ${eventType}" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input
            v-model="templateForm.body"
            type="textarea"
            :rows="4"
            placeholder="事件 ${eventType} 发生在 ${location}"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="templateDialog = false">取消</el-button>
        <el-button type="primary" @click="saveTemplate">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createChannel,
  createTemplate,
  listChannels,
  listNotificationDeliveries,
  listTemplates,
  resendNotification
} from '@/api'
import type { ChannelView, DeliveryView, TemplateView } from '@/types'

const activeTab = ref('channels')
const channels = ref<ChannelView[]>([])
const templates = ref<TemplateView[]>([])
const deliveries = ref<DeliveryView[]>([])
const eventIdFilter = ref('')

const channelDialog = ref(false)
const templateDialog = ref(false)
const channelForm = reactive({ name: '', type: 'WEBHOOK', url: '' })
const templateForm = reactive({ name: '', channelType: 'WEBHOOK', subject: '', body: '' })

async function loadChannels() {
  channels.value = await listChannels()
}

async function loadTemplates() {
  templates.value = await listTemplates()
}

async function loadDeliveries() {
  deliveries.value = await listNotificationDeliveries(eventIdFilter.value || undefined)
}

async function saveChannel() {
  await createChannel({
    name: channelForm.name,
    type: channelForm.type,
    config: { url: channelForm.url },
    enabled: true
  })
  channelDialog.value = false
  ElMessage.success('通道已创建')
  loadChannels()
}

async function saveTemplate() {
  await createTemplate({ ...templateForm })
  templateDialog.value = false
  ElMessage.success('模板已创建')
  loadTemplates()
}

async function resend(id: number) {
  await resendNotification(id)
  ElMessage.success('已重发')
  loadDeliveries()
}

watch(activeTab, (tab) => {
  if (tab === 'deliveries') {
    loadDeliveries()
  }
})

onMounted(() => {
  loadChannels()
  loadTemplates()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}
</style>
