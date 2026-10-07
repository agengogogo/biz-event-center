<template>
  <el-container class="layout">
    <el-aside width="220px" class="sidebar">
      <div class="brand">园区事件中心</div>
      <el-menu :default-active="activeMenu" router class="menu">
        <el-menu-item index="/events">事件管理</el-menu-item>
        <el-menu-item index="/sources">来源管理</el-menu-item>
        <el-menu-item index="/subscriptions">订阅与规则</el-menu-item>
        <el-menu-item index="/notifications">通知管理</el-menu-item>
        <el-menu-item index="/dashboard">运营大屏</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="park">当前园区：{{ auth.parkId }}</span>
        <span class="user">{{ auth.roles.join(' / ') }}</span>
        <el-button link type="primary" @click="handleLogout">退出登录</el-button>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const activeMenu = computed(() => '/' + (route.path.split('/')[1] ?? 'events'))

function handleLogout() {
  auth.logout()
  router.push({ name: 'login' })
}
</script>

<style scoped>
.layout {
  height: 100vh;
}

.sidebar {
  background: #1f2d3d;
  color: #fff;
}

.brand {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 16px;
  color: #fff;
}

.menu {
  border-right: none;
  background: transparent;
}

.header {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
}

.park {
  font-weight: 600;
}

.user {
  color: #909399;
}
</style>
