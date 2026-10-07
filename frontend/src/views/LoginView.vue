<template>
  <div class="login-page">
    <el-card class="login-card">
      <template #header>园区业务事件中心</template>
      <el-form :model="form" label-position="top" @submit.prevent>
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="admin / ops" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-button type="primary" :loading="loading" class="submit" @click="handleLogin">
          登录
        </el-button>
        <p v-if="error" class="error">{{ error }}</p>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const form = reactive({ username: 'admin', password: 'admin123' })
const loading = ref(false)
const error = ref('')

async function handleLogin() {
  loading.value = true
  error.value = ''
  try {
    await auth.login(form.username, form.password)
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) ?? '/events'
    router.push(redirect)
  } catch {
    error.value = '登录失败：用户名或密码错误'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f5;
}

.login-card {
  width: 360px;
}

.submit {
  width: 100%;
}

.error {
  color: #f56c6c;
  margin-top: 12px;
}
</style>
