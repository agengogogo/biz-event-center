<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>来源管理</span>
        <el-button type="primary" @click="dialogVisible = true">注册来源</el-button>
      </div>
    </template>

    <el-alert
      v-if="createdSecret"
      type="success"
      show-icon
      :closable="false"
      class="secret"
      title="来源凭证仅在注册时展示一次，请妥善保存"
    >
      {{ createdSecret }}
    </el-alert>

    <el-table v-loading="loading" :data="sources" border>
      <el-table-column prop="name" label="名称" min-width="160" />
      <el-table-column prop="sourceType" label="类型" width="140" />
      <el-table-column prop="parkId" label="园区" width="120" />
      <el-table-column prop="enabled" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'">
            {{ row.enabled ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="注册来源" width="420px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.sourceType" style="width: 100%">
            <el-option label="IOT" value="IOT" />
            <el-option label="ACCESS" value="ACCESS" />
            <el-option label="ENERGY" value="ENERGY" />
            <el-option label="PARKING" value="PARKING" />
            <el-option label="SECURITY" value="SECURITY" />
            <el-option label="THIRD_PARTY" value="THIRD_PARTY" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createSource, deleteSource, listSources } from '@/api'
import type { SourceView } from '@/types'

const sources = ref<SourceView[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const createdSecret = ref('')
const form = reactive({ name: '', sourceType: 'IOT' })

async function load() {
  loading.value = true
  try {
    sources.value = await listSources()
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  const source = await createSource({ ...form, enabled: true })
  createdSecret.value = `来源ID: ${source.id}    凭证: ${source.secret}`
  dialogVisible.value = false
  form.name = ''
  ElMessage.success('注册成功')
  load()
}

async function handleDelete(id: string) {
  await ElMessageBox.confirm('确认删除该来源？', '提示', { type: 'warning' })
  await deleteSource(id)
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

.secret {
  margin-bottom: 12px;
}
</style>
